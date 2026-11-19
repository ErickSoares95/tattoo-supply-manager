package com.ericksoares.tattoo.user;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute-force protection for POST /auth/login (U9): after {@value #MAX_FAILURES} wrong
 * passwords for the same login within {@link #WINDOW}, that login is locked for
 * {@link #LOCK}, even with the right password (429).
 *
 * <p>Keyed by login, not by IP: the storefront logs in from its own server (BFF), so every
 * customer reaches the API from the same few Vercel IPs and an IP limit would lock them all
 * out together. In-memory, so it is per instance and resets on restart - enough for the
 * single Render instance; several instances would need a shared store (Redis, a table).</p>
 */
@Component
class LoginAttemptGuard {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    static final Duration LOCK = Duration.ofMinutes(15);
    private static final int PRUNE_THRESHOLD = 10_000;

    private record Attempts(int failures, Instant firstFailureAt, Instant lockedUntil) {}

    private final Map<String, Attempts> attemptsByLogin = new ConcurrentHashMap<>();

    private Clock clock = Clock.systemUTC();

    void useClock(Clock clock) {
        this.clock = clock;
    }

    void ensureNotLocked(String login) {
        Attempts attempts = attemptsByLogin.get(key(login));
        if (attempts != null && attempts.lockedUntil() != null && clock.instant().isBefore(attempts.lockedUntil())) {
            long minutes = Math.max(1, Duration.between(clock.instant(), attempts.lockedUntil()).toMinutes());
            throw new TooManyLoginAttemptsException(minutes);
        }
    }

    void recordFailure(String login) {
        Instant now = clock.instant();
        attemptsByLogin.compute(key(login), (k, current) -> {
            if (current == null || now.isAfter(current.firstFailureAt().plus(WINDOW))) {
                return new Attempts(1, now, null);
            }
            int failures = current.failures() + 1;
            Instant lockedUntil = failures >= MAX_FAILURES ? now.plus(LOCK) : current.lockedUntil();
            return new Attempts(failures, current.firstFailureAt(), lockedUntil);
        });
        if (attemptsByLogin.size() > PRUNE_THRESHOLD) {
            prune(now);
        }
    }

    void recordSuccess(String login) {
        attemptsByLogin.remove(key(login));
    }

    private void prune(Instant now) {
        attemptsByLogin.entrySet().removeIf(entry -> {
            Attempts attempts = entry.getValue();
            Instant expiry = attempts.lockedUntil() != null ? attempts.lockedUntil() : attempts.firstFailureAt().plus(WINDOW);
            return now.isAfter(expiry);
        });
    }

    private static String key(String login) {
        return login == null ? "" : login.trim().toLowerCase(Locale.ROOT);
    }
}
