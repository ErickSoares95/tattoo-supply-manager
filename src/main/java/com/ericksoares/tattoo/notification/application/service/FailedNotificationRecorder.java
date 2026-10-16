package com.ericksoares.tattoo.notification.application.service;

import com.ericksoares.tattoo.notification.application.dto.SenderFailure;
import com.ericksoares.tattoo.notification.domain.entity.FailedNotification;
import com.ericksoares.tattoo.notification.domain.repository.FailedNotificationRepository;
import com.ericksoares.tattoo.notification.domain.exception.PermanentNotificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Persists the bookkeeping of failed deliveries (one row per order and sender).
 *
 * <p>Every method opens its own transaction: callers run after the order's transaction has
 * committed (AFTER_COMMIT listener) or on the rejecting thread of the executor, where joining
 * the previous transaction would silently drop the write.
 */
@Slf4j
@Component
public class FailedNotificationRecorder {

    private final FailedNotificationRepository repository;

    public FailedNotificationRecorder(FailedNotificationRepository repository) {
        this.repository = repository;
    }

    /**
     * A first delivery attempt just failed: record it as attempt 1, retried after the first backoff.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FailedNotification recordFailure(Long orderId, SenderFailure failure) {

        FailedNotification row = newRow(orderId, failure.senderName());
        row.setAttempts(0);

        registerAttemptFailure(row, failure.error());
        return row;
    }

    /**
     * The executor refused the task before any delivery was attempted: record it with zero
     * attempts, due right away for the scheduled retry.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FailedNotification recordRejected(Long orderId, String senderName) {

        FailedNotification row = newRow(orderId, senderName);
        row.setAttempts(0);
        row.setNextAttemptAt(Instant.now());
        row.setErrorMessage("Notification executor rejected the task (queue full or shutting down)");

        return repository.save(row);
    }

    /**
     * Another attempt on an existing row failed: count it and reschedule (or park it).
     *
     * <p>The row is re-read by id inside this transaction instead of writing back the caller's
     * (detached, possibly stale) entity, so a concurrent change such as {@code processed=true}
     * is never overwritten. The lease is released so the row is due again at {@code nextAttemptAt}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRetryFailure(Long id, Exception error) {

        FailedNotification row = repository.findById(id).orElse(null);

        if (row == null || Boolean.TRUE.equals(row.getProcessed())) {
            log.warn("Retry failure for id {} ignored: row is gone or already processed", id);
            return;
        }

        row.setLockedUntil(null);
        registerAttemptFailure(row, error);
    }

    private void registerAttemptFailure(FailedNotification row, Exception error) {

        if (error instanceof PermanentNotificationException) {
            // Retrying cannot fix it: park as terminal (attempts at the limit, no next attempt)
            // so it drops out of every retry query and waits for manual review.
            row.setAttempts(NotificationRetryPolicy.MAX_ATTEMPTS);
            row.setNextAttemptAt(null);
            row.setErrorMessage(describe(error));

            log.error("Permanent failure for order {} on sender {} - not retried, manual review needed: {}",
                    row.getOrderId(), row.getSenderName(), error.getMessage());

            repository.save(row);
            return;
        }

        int attempts = row.attemptCount() + 1;

        row.setAttempts(attempts);
        row.setErrorMessage(describe(error));
        row.setNextAttemptAt(NotificationRetryPolicy.nextAttemptAt(attempts, Instant.now()));

        if (NotificationRetryPolicy.isExhausted(attempts)) {
            log.error("Notification for order {} on sender {} exhausted {} attempts - manual review needed",
                    row.getOrderId(), row.getSenderName(), attempts);
        }

        repository.save(row);
    }

    private FailedNotification newRow(Long orderId, String senderName) {

        FailedNotification row = new FailedNotification();

        row.setOrderId(orderId);
        row.setSenderName(senderName);
        row.setCreatedAt(LocalDateTime.now());
        row.setProcessed(false);

        return row;
    }

    private String describe(Exception error) {
        if (error == null) {
            return "Failed to deliver notification";
        }
        String message = error.getClass().getSimpleName() + ": " + error.getMessage();
        return message.length() > 255 ? message.substring(0, 255) : message;
    }
}
