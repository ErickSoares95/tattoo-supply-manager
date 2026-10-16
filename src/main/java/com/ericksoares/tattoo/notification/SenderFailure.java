package com.ericksoares.tattoo.notification;

/**
 * A single sender that did not manage to deliver a notification, with the cause.
 */
record SenderFailure(
        String senderName,
        Exception error
) {}
