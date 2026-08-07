package com.ericksoares.tattoo.notification.infrastructure.email;

import com.ericksoares.tattoo.notification.application.listener.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailNotificationSender implements NotificationSender {
    @Override
    public void sendOrderRegistered(Long orderId) {
        log.info("Sending fake email for order {}", orderId);
    }

    @Override
    public void sendPaymentConfirmed(Long orderId) {
        log.info("Sending fake email: payment confirmed for order {}", orderId);
    }

    @Override
    public void sendPaymentRejected(Long orderId) {
        log.info("Sending fake email: payment rejected for order {}", orderId);
    }

    /**
     * Not part of the NotificationSender interface on purpose - a welcome email is
     * inherently email-only (unlike order/payment notifications, it doesn't make sense
     * to fan it out to webhook/WhatsApp too), so this is called directly by
     * notification.event.WelcomeEmailListener instead of through the sender list.
     */
    public void sendWelcomeEmail(String email, String fullName) {
        log.info("Sending fake welcome email to {} ({})", email, fullName);
    }
}