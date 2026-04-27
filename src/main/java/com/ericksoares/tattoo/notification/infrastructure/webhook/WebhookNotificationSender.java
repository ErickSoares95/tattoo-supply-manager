package com.ericksoares.tattoo.notification.infrastructure.webhook;

import com.ericksoares.tattoo.notification.application.port.NotificationSender;
import org.springframework.stereotype.Service;

@Service
public class WebhookNotificationSender implements NotificationSender {

    @Override
    public void sendOrderRegistered(Long orderId) {
        System.out.println("🔗 Sending webhook for order: " + orderId);
    }
}
