package com.ericksoares.tattoo.notification.application.service;

import com.ericksoares.tattoo.notification.domain.exception.PermanentNotificationException;
import com.ericksoares.tattoo.notification.domain.exception.TransientNotificationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Decides whether a sender failure is worth retrying. Network problems, timeouts, 5xx, 408
 * and 429 are transient; invalid payloads and other 4xx are permanent. Anything unrecognised is
 * treated as transient - bounded by the maximum attempts, so it cannot retry forever.
 */
public final class NotificationErrorClassifier {

    private NotificationErrorClassifier() {
    }

    public static RuntimeException classify(Exception error) {

        if (error instanceof TransientNotificationException transientError) {
            return transientError;
        }

        if (error instanceof PermanentNotificationException permanentError) {
            return permanentError;
        }

        if (error instanceof HttpClientErrorException clientError) {
            HttpStatusCode status = clientError.getStatusCode();
            boolean retryable = status.value() == 408 || status.value() == 429;

            return retryable
                    ? new TransientNotificationException("Sender returned " + status, error)
                    : new PermanentNotificationException("Sender rejected the request: " + status, error);
        }

        if (error instanceof HttpServerErrorException
                || error instanceof ResourceAccessException
                || error instanceof IOException
                || error instanceof TimeoutException) {
            return new TransientNotificationException("Temporary delivery failure: " + error.getMessage(), error);
        }

        if (error instanceof IllegalArgumentException) {
            return new PermanentNotificationException("Invalid notification payload: " + error.getMessage(), error);
        }

        return new TransientNotificationException("Delivery failed: " + error.getMessage(), error);
    }
}
