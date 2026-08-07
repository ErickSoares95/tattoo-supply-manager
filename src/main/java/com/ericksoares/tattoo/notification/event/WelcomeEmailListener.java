package com.ericksoares.tattoo.notification.event;

import com.ericksoares.tattoo.notification.infrastructure.email.EmailNotificationSender;
import com.ericksoares.tattoo.user.domain.event.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Same in-process pattern as notification.event.NotificationListener (order flow):
 * @Async + @TransactionalEventListener(AFTER_COMMIT), so registration never waits
 * on the email. No FailedNotification-style persistence here on purpose - a missed
 * welcome email is low-stakes (unlike a missed order/payment notification), so a
 * logged failure is enough; no retry/DLQ infrastructure for this one.
 */
@Slf4j
@Component
public class WelcomeEmailListener {

    private final EmailNotificationSender emailSender;

    public WelcomeEmailListener(EmailNotificationSender emailSender) {
        this.emailSender = emailSender;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserRegisteredEvent event) {

        log.info("Received UserRegisteredEvent for user {}", event.userId());

        try {
            emailSender.sendWelcomeEmail(event.email(), event.fullName());
        } catch (Exception e) {
            log.error("Failed to send welcome email to user {}", event.userId(), e);
        }
    }
}
