package com.ericksoares.tattoo.notification.infrastructure.webhook;

import com.ericksoares.tattoo.notification.application.listener.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WebhookNotificationSender implements NotificationSender {

    @Override
    public void sendOrderRegistered(Long orderId) {
        log.info("Sending webhook for order {}", orderId);
    }
}
