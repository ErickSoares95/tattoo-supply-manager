package com.ericksoares.tattoo.notification.listener;

import com.ericksoares.tattoo.notification.application.service.SendNotificationService;
import com.ericksoares.tattoo.order.domain.event.OrderRegisteredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class OrderRegisteredListener {

    private final SendNotificationService notificationService;

    public OrderRegisteredListener(SendNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Async
    @EventListener
    public void handle(OrderRegisteredEvent event) {

        String message = "New order created. Total: " + event.total();

        notificationService.sendOrderRegistered(message);
    }
}