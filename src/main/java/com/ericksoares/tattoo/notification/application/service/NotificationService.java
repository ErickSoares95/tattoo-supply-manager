package com.ericksoares.tattoo.notification.application.service;

import com.ericksoares.tattoo.notification.application.port.NotificationSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final List<NotificationSender> senders;

    public NotificationService(List<NotificationSender> senders) {
        this.senders = senders;
    }

    public void notifyOrderRegistered(Long orderId) {
        for (NotificationSender sender : senders) {
            sender.sendOrderRegistered(orderId);
        }
    }
}
