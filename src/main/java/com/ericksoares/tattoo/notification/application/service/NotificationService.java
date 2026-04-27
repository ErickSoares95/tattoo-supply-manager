package com.ericksoares.tattoo.notification.application.service;

import com.ericksoares.tattoo.notification.application.dto.NotificationContext;
import com.ericksoares.tattoo.notification.application.listener.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class NotificationService {

    private final List<NotificationSender> senders;

    public NotificationService(List<NotificationSender> senders) {
        this.senders = senders;
    }

    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void notifyOrderRegistered(NotificationContext context) {
        for (NotificationSender sender : senders) {
            sender.sendOrderRegistered(context.orderId());
        }
    }

    @Recover
    public void recover(Exception e, NotificationContext context) {
        log.error(
                "FINAL FAILURE - orderId: {}, product: {}, quantity: {}",
                context.orderId(),
                context.productName(),
                context.quantity(),
                e
        );
    }
}
