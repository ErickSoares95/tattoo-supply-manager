package com.ericksoares.tattoo.notification.domain.repository;

import com.ericksoares.tattoo.notification.domain.entity.FailedNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface FailedNotificationRepository
        extends JpaRepository<FailedNotification, Long> {

    /**
     * Unprocessed failures that still have attempts left and are not leased by another
     * worker ({@code lockedUntil} null or elapsed), oldest first, keyset-paginated by id
     * (pass the last id seen as {@code afterId}, 0 for the first page).
     */
    @Query("""
            select f from FailedNotification f
            where f.processed = false
              and coalesce(f.attempts, 0) < :maxAttempts
              and (f.lockedUntil is null or f.lockedUntil <= :now)
              and f.id > :afterId
            order by f.id
            """)
    List<FailedNotification> findRetryable(
            @Param("maxAttempts") int maxAttempts,
            @Param("now") Instant now,
            @Param("afterId") Long afterId,
            Pageable pageable
    );

    /**
     * Same as {@link #findRetryable} but only rows whose backoff has elapsed
     * ({@code nextAttemptAt} null or not after {@code now}) - what the scheduled job retries.
     */
    @Query("""
            select f from FailedNotification f
            where f.processed = false
              and coalesce(f.attempts, 0) < :maxAttempts
              and (f.nextAttemptAt is null or f.nextAttemptAt <= :now)
              and (f.lockedUntil is null or f.lockedUntil <= :now)
              and f.id > :afterId
            order by f.id
            """)
    List<FailedNotification> findDue(
            @Param("maxAttempts") int maxAttempts,
            @Param("now") Instant now,
            @Param("afterId") Long afterId,
            Pageable pageable
    );

    /**
     * Atomically leases a row until {@code until}: the single UPDATE is the arbiter between the
     * scheduled job, {@code POST /notifications/reprocess} and other instances. Returns 1 when
     * this caller won the lease (and may send), 0 when the row is processed or leased by someone else.
     */
    @Transactional
    @Modifying
    @Query("""
            update FailedNotification f
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
    @Query("update FailedNotification f set f.processed = true, f.lockedUntil = null where f.id = :id")
    int markProcessed(@Param("id") Long id);
}
