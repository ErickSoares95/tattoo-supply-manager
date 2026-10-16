package com.ericksoares.tattoo.notification.domain.repository;

import com.ericksoares.tattoo.notification.domain.entity.FailedPaymentNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface FailedPaymentNotificationRepository
        extends JpaRepository<FailedPaymentNotification, Long> {

    List<FailedPaymentNotification> findByProcessedFalse();

    /**
     * Unprocessed failures that still have attempts left and are not leased by another
     * worker, oldest first, keyset-paginated by id (0 for the first page).
     */
    @Query("""
            select f from FailedPaymentNotification f
            where f.processed = false
              and coalesce(f.attempts, 0) < :maxAttempts
              and (f.lockedUntil is null or f.lockedUntil <= :now)
              and f.id > :afterId
            order by f.id
            """)
    List<FailedPaymentNotification> findRetryable(
            @Param("maxAttempts") int maxAttempts,
            @Param("now") Instant now,
            @Param("afterId") Long afterId,
            Pageable pageable
    );

    /**
     * Same as {@link #findRetryable} but only rows whose backoff has elapsed - what the scheduled job retries.
     */
    @Query("""
            select f from FailedPaymentNotification f
            where f.processed = false
              and coalesce(f.attempts, 0) < :maxAttempts
              and (f.nextAttemptAt is null or f.nextAttemptAt <= :now)
              and (f.lockedUntil is null or f.lockedUntil <= :now)
              and f.id > :afterId
            order by f.id
            """)
    List<FailedPaymentNotification> findDue(
            @Param("maxAttempts") int maxAttempts,
            @Param("now") Instant now,
            @Param("afterId") Long afterId,
            Pageable pageable
    );

    /**
     * Atomically leases a row until {@code until}; returns 1 when this caller won the lease,
     * 0 when the row is processed or leased by someone else. See FailedNotificationRepository#claim.
     */
    @Transactional
    @Modifying
    @Query("""
            update FailedPaymentNotification f
            set f.lockedUntil = :until
            where f.id = :id
              and f.processed = false
              and (f.lockedUntil is null or f.lockedUntil < :now)
            """)
    int claim(@Param("id") Long id, @Param("until") Instant until, @Param("now") Instant now);

    /**
     * Marks a row as delivered and drops its lease, without writing back a possibly stale entity.
     */
    @Transactional
    @Modifying
    @Query("update FailedPaymentNotification f set f.processed = true, f.lockedUntil = null where f.id = :id")
    int markProcessed(@Param("id") Long id);
}
