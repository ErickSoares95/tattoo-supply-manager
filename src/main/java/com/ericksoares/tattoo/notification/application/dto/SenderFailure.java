package com.ericksoares.tattoo.notification.application.dto;

/**
 * A single sender that did not manage to deliver a notification, with the cause.
 */
public record SenderFailure(
        String senderName,
        Exception error
) {}
