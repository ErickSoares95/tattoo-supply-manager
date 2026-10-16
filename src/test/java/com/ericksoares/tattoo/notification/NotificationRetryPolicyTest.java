package com.ericksoares.tattoo.notification;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class NotificationRetryPolicyTest {

    @Test
    void shouldDoubleTheDelayAtEachFailedAttempt() {

        assertEquals(Duration.ofSeconds(30), NotificationRetryPolicy.backoff(1));
        assertEquals(Duration.ofSeconds(60), NotificationRetryPolicy.backoff(2));
        assertEquals(Duration.ofSeconds(120), NotificationRetryPolicy.backoff(3));
        assertEquals(Duration.ofSeconds(240), NotificationRetryPolicy.backoff(4));
    }

    @Test
    void shouldCapTheDelay() {

        assertEquals(Duration.ofHours(1), NotificationRetryPolicy.backoff(30));
        assertEquals(Duration.ofHours(1), NotificationRetryPolicy.backoff(Integer.MAX_VALUE));
    }

    @Test
    void shouldScheduleNextAttemptRelativeToNow() {

        Instant now = Instant.parse("2026-10-16T12:00:00Z");

        assertEquals(now.plusSeconds(60), NotificationRetryPolicy.nextAttemptAt(2, now));
    }

    @Test
    void shouldBeExhaustedAtMaxAttempts() {

        assertFalse(NotificationRetryPolicy.isExhausted(NotificationRetryPolicy.MAX_ATTEMPTS - 1));
        assertTrue(NotificationRetryPolicy.isExhausted(NotificationRetryPolicy.MAX_ATTEMPTS));
    }
}
