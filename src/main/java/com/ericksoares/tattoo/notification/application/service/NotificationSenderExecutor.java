package com.ericksoares.tattoo.notification.application.service;

import com.ericksoares.tattoo.notification.application.listener.NotificationSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class NotificationSenderExecutor {

    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void send(NotificationSender sender, Long orderId) {
        sender.sendOrderRegistered(orderId);
    }
}
