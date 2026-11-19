package com.ericksoares.tattoo.notification;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationTimingValidatorTest {

    @Test
    void acceptsATimeoutThatFitsInTheLeaseWithMargin() {
        assertDoesNotThrow(() -> NotificationTimingValidator.check(Duration.ofSeconds(30), Duration.ofMinutes(2)));
    }

    @Test
    void refusesATimeoutTooCloseToTheLease() {
        assertThrows(IllegalStateException.class,
                () -> NotificationTimingValidator.check(Duration.ofSeconds(100), Duration.ofMinutes(2)));
    }
}
