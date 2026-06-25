package com.ericksoares.tattoo.notification.event;

import com.ericksoares.tattoo.notification.application.dto.NotificationContext;
import com.ericksoares.tattoo.notification.application.service.NotificationService;
import com.ericksoares.tattoo.order.application.dto.OrderItemData;
import com.ericksoares.tattoo.order.domain.event.OrderRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationListener  {

    private final NotificationService notificationService;

    public NotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    @Async("notificationExecutor")
    @EventListener
    public void handle(OrderRegisteredEvent event) {

        log.info("Received OrderRegisteredEvent for order {}", event.orderId());
        log.info("Processing in thread: {}", Thread.currentThread().getName());

        try {
            for (OrderItemData item : event.items()) {

                NotificationContext context = new NotificationContext(
                        event.orderId(),
                        item.productName(),
                        item.quantity()
                );

                notificationService.notifyOrderRegistered(context);
            }

        } catch (Exception e) {
            log.error("Failed to process notification for order {}", event.orderId(), e);
        }
    }
}
