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
}