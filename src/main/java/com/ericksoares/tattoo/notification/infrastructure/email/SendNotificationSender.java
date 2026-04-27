package com.ericksoares.tattoo.notification.infrastructure.email;

import com.ericksoares.tattoo.notification.application.port.NotificationSender;
import org.springframework.stereotype.Service;

@Service
public class SendNotificationSender implements NotificationSender {
    @Override
    public void sendOrderRegistered(Long orderId) {
        System.out.println("📧 Sending email for order: " + orderId);
    }
}