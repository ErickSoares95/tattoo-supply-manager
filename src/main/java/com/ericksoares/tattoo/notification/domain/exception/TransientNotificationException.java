package com.ericksoares.tattoo.notification.domain.exception;

/**
 * Delivery failed for a reason that may go away (network error, timeout, 5xx, rate limit):
 * the notification is rescheduled with exponential backoff.
 */
public class TransientNotificationException extends RuntimeException {

    public TransientNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
