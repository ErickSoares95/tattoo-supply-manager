package com.ericksoares.tattoo.notification.application.service;

import com.ericksoares.tattoo.notification.application.dto.SenderFailure;
import com.ericksoares.tattoo.notification.domain.entity.FailedPaymentNotification;
import com.ericksoares.tattoo.notification.domain.exception.PermanentNotificationException;
import com.ericksoares.tattoo.notification.domain.repository.FailedPaymentNotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persists the bookkeeping of failed payment notifications (one row per payment event and
 * sender), same rules as {@link FailedNotificationRecorder}. Every method opens its own
 * transaction because callers (the Kafka listener, the retry job) have none to join.
 */
@Slf4j
@Component
public class FailedPaymentNotificationRecorder {

    private final FailedPaymentNotificationRepository repository;

    public FailedPaymentNotificationRecorder(FailedPaymentNotificationRepository repository) {
        this.repository = repository;
    }

    /**
     * A first delivery attempt just failed: record it as attempt 1, retried after the first backoff.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FailedPaymentNotification recordFailure(
            UUID eventId, Long orderId, String status, BigDecimal amount, SenderFailure failure
    ) {

        FailedPaymentNotification row = new FailedPaymentNotification();

        row.setEventId(eventId);
        row.setOrderId(orderId);
        row.setStatus(status);
        row.setAmount(amount);
        row.setSenderName(failure.senderName());
        row.setCreatedAt(LocalDateTime.now());
        row.setProcessed(false);
        row.setAttempts(0);

        registerAttemptFailure(row, failure.error());
        return row;
    }

    /**
     * Another attempt on an existing row failed: count it and reschedule (or park it). The row is
     * re-read by id so a concurrent {@code processed=true} is never overwritten; the lease is released.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRetryFailure(Long id, Exception error) {

        FailedPaymentNotification row = repository.findById(id).orElse(null);

        if (row == null || Boolean.TRUE.equals(row.getProcessed())) {
            log.warn("Retry failure for payment notification {} ignored: row is gone or already processed", id);
            return;
        }

        row.setLockedUntil(null);
        registerAttemptFailure(row, error);
    }

    private void registerAttemptFailure(FailedPaymentNotification row, Exception error) {

        if (error instanceof PermanentNotificationException) {
            // Retrying cannot fix it: park as terminal for manual review.
            row.setAttempts(NotificationRetryPolicy.MAX_ATTEMPTS);
            row.setNextAttemptAt(null);
            row.setErrorMessage(describe(error));

            log.error("Permanent failure for payment of order {} on sender {} - not retried, manual review needed: {}",
                    row.getOrderId(), row.getSenderName(), error.getMessage());

            repository.save(row);
            return;
        }

        int attempts = row.attemptCount() + 1;

        row.setAttempts(attempts);
        row.setErrorMessage(describe(error));
        row.setNextAttemptAt(NotificationRetryPolicy.nextAttemptAt(attempts, Instant.now()));

        if (NotificationRetryPolicy.isExhausted(attempts)) {
            log.error("Payment notification for order {} on sender {} exhausted {} attempts - manual review needed",
                    row.getOrderId(), row.getSenderName(), attempts);
        }

        repository.save(row);
    }

    private String describe(Exception error) {
        if (error == null) {
            return "Failed to deliver notification";
        }
        String message = error.getClass().getSimpleName() + ": " + error.getMessage();
        return message.length() > 255 ? message.substring(0, 255) : message;
    }
}
