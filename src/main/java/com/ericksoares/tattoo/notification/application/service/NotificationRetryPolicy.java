package com.ericksoares.tattoo.notification.application.service;

import java.time.Duration;
import java.time.Instant;

/**
 * Retry rules for failed notifications: exponential backoff (30s * 2^(attempts-1), capped)
 * and a maximum number of delivery attempts, after which a failure is terminal and waits
 * for manual review.
 */
public final class NotificationRetryPolicy {

    public static final int MAX_ATTEMPTS = 5;

    static final Duration BASE_DELAY = Duration.ofSeconds(30);
    static final Duration MAX_DELAY = Duration.ofHours(1);

    private NotificationRetryPolicy() {
    }

    /**
     * Delay before the next attempt, given how many attempts have already failed (>= 1).
     */
    public static Duration backoff(int failedAttempts) {
        int exponent = Math.max(0, Math.min(failedAttempts - 1, 20));
        Duration delay = BASE_DELAY.multipliedBy(1L << exponent);
        return delay.compareTo(MAX_DELAY) > 0 ? MAX_DELAY : delay;
    }

    public static Instant nextAttemptAt(int failedAttempts, Instant now) {
        return now.plus(backoff(failedAttempts));
    }

    public static boolean isExhausted(int attempts) {
        return attempts >= MAX_ATTEMPTS;
    }
}
