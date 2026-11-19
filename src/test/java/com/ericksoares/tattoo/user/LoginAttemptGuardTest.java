package com.ericksoares.tattoo.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptGuardTest {

    private final LoginAttemptGuard guard = new LoginAttemptGuard();
    private Instant now = Instant.parse("2026-11-19T12:00:00Z");

    @BeforeEach
    void fixClock() {
        useNow();
    }

    private void useNow() {
        guard.useClock(Clock.fixed(now, ZoneOffset.UTC));
    }

    private void advance(Duration duration) {
        now = now.plus(duration);
        useNow();
    }

    private void fail(int times) {
        for (int i = 0; i < times; i++) {
            guard.recordFailure("client@demo.com");
        }
    }

    @Test
    void locksAfterMaxFailuresAndUnlocksAfterTheLockPeriod() {
        fail(LoginAttemptGuard.MAX_FAILURES - 1);
        assertDoesNotThrow(() -> guard.ensureNotLocked("client@demo.com"));

        fail(1);
        assertThrows(TooManyLoginAttemptsException.class, () -> guard.ensureNotLocked("client@demo.com"));

        advance(LoginAttemptGuard.LOCK.plusSeconds(1));
        assertDoesNotThrow(() -> guard.ensureNotLocked("client@demo.com"));
    }

    @Test
    void failuresOutsideTheWindowStartANewCount() {
        fail(LoginAttemptGuard.MAX_FAILURES - 1);
        advance(LoginAttemptGuard.WINDOW.plusSeconds(1));
        fail(1);

        assertDoesNotThrow(() -> guard.ensureNotLocked("client@demo.com"));
    }

    @Test
    void successClearsTheFailures() {
        fail(LoginAttemptGuard.MAX_FAILURES - 1);
        guard.recordSuccess("client@demo.com");
        fail(1);

        assertDoesNotThrow(() -> guard.ensureNotLocked("client@demo.com"));
    }

    @Test
    void otherLoginsAreNotAffected() {
        fail(LoginAttemptGuard.MAX_FAILURES);

        assertDoesNotThrow(() -> guard.ensureNotLocked("admin@demo.com"));
    }
}
