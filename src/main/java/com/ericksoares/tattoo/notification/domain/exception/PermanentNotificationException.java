package com.ericksoares.tattoo.notification.domain.exception;

/**
 * Delivery failed for a reason retrying cannot fix (invalid payload, 4xx, unknown channel):
 * never retried, the failure is parked for manual review.
 */
public class PermanentNotificationException extends RuntimeException {

    public PermanentNotificationException(String message) {
        super(message);
    }

    public PermanentNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
