package com.ericksoares.tattoo.notification.application.service;

import com.ericksoares.tattoo.notification.application.dto.NotificationContext;
import com.ericksoares.tattoo.notification.application.listener.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class NotificationService {

    private final List<NotificationSender> senders;
    private final NotificationSenderExecutor executor;

    public NotificationService(List<NotificationSender> senders, NotificationSenderExecutor executor) {
        this.senders = senders;
        this.executor = executor;
    }

    public boolean notifyOrderRegistered(NotificationContext context) {

        log.info(
                "Sending notification for order {}",
                context.orderId()
        );

        boolean allSucceeded = true;

        for (NotificationSender sender : senders) {

            try {
                executor.send(sender, context.orderId());
            } catch (Exception e) {
                allSucceeded = false;
                log.error(
                        "FINAL FAILURE - sender: {}, orderId: {}, product: {}, quantity: {}",
                        sender.getClass().getSimpleName(),
                        context.orderId(),
                        context.productName(),
                        context.quantity(),
                        e
                );
            }
        }

        return allSucceeded;
    }
}
