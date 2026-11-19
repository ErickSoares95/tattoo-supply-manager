package com.ericksoares.tattoo.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * N13: a resend holds a 2-minute lease on its failure row (NotificationReprocessService.LEASE)
 * and no transaction during the network call. If a sender could take longer than the lease, the
 * lease would expire mid-call and another run (or instance) could claim and send the same
 * notification again. Today's senders only log; a real one (SMTP, HTTP webhook, WhatsApp API)
 * must use {@code notification.sender.timeout} as its connect+read timeout, and this check
 * refuses to start the app when that timeout doesn't fit inside the lease with a safety margin.
 */
@Component
class NotificationTimingValidator {

    static final Duration SAFETY_MARGIN = Duration.ofSeconds(30);

    private final Duration senderTimeout;

    NotificationTimingValidator(@Value("${notification.sender.timeout:30s}") Duration senderTimeout) {
        this.senderTimeout = senderTimeout;
    }

    @EventListener(ApplicationReadyEvent.class)
    void validate() {
        check(senderTimeout, NotificationReprocessService.LEASE);
    }

    static void check(Duration senderTimeout, Duration lease) {
        if (senderTimeout.plus(SAFETY_MARGIN).compareTo(lease) > 0) {
            throw new IllegalStateException("notification.sender.timeout (" + senderTimeout
                    + ") plus a " + SAFETY_MARGIN + " margin must fit inside the resend lease (" + lease
                    + "), or a slow send could be claimed and sent twice");
        }
    }
}
