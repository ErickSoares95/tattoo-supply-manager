package com.ericksoares.tattoo.notification;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * One unprocessed notification failure, order or payment (N11). {@code parked} = terminal:
 * a permanent error or out of attempts, never retried again - it needs a person.
 */
record FailedNotificationView(
        Long id,
        String type,
        Long orderId,
        String senderName,
        int attempts,
        int maxAttempts,
        Instant nextAttemptAt,
        boolean parked,
        String errorMessage,
        LocalDateTime createdAt
) {}
