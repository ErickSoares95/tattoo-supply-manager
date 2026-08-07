package com.ericksoares.tattoo.notification.infrastructure.whatsapp;

import com.ericksoares.tattoo.notification.application.listener.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WhatsAppNotificationSender implements NotificationSender {

    @Override
    public void sendOrderRegistered(Long orderId) {
        log.info("Sending WhatsApp message for order {}", orderId);
    }

    @Override
    public void sendPaymentConfirmed(Long orderId) {
        log.info("Sending WhatsApp message: payment confirmed for order {}", orderId);
    }

    @Override
    public void sendPaymentRejected(Long orderId) {
        log.info("Sending WhatsApp message: payment rejected for order {}", orderId);
    }
}
