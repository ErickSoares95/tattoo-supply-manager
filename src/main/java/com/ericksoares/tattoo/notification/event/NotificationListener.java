package com.ericksoares.tattoo.notification.event;

import com.ericksoares.tattoo.notification.application.dto.NotificationContext;
import com.ericksoares.tattoo.notification.application.service.NotificationService;
import com.ericksoares.tattoo.notification.domain.entity.FailedNotification;
import com.ericksoares.tattoo.notification.domain.repository.FailedNotificationRepository;
import com.ericksoares.tattoo.order.application.dto.OrderItemData;
import com.ericksoares.tattoo.order.domain.event.OrderRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Slf4j
@Component
public class NotificationListener  {

    private final NotificationService notificationService;
    private final FailedNotificationRepository failedRepository;

    public NotificationListener(
            NotificationService notificationService,
            FailedNotificationRepository failedRepository
    ) {
        this.notificationService = notificationService;
        this.failedRepository = failedRepository;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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

                boolean succeeded = notificationService.notifyOrderRegistered(context);

                if (!succeeded) {
                    persistFailure(context);
                }
            }

        } catch (Exception e) {
            log.error("Failed to process notification for order {}", event.orderId(), e);
        }
    }

    private void persistFailure(NotificationContext context) {

        FailedNotification failure = new FailedNotification();

        failure.setOrderId(context.orderId());
        failure.setProductName(context.productName());
        failure.setQuantity(context.quantity());
        failure.setErrorMessage("Failed to deliver notification to one or more senders");
        failure.setCreatedAt(LocalDateTime.now());
        failure.setProcessed(false);

        failedRepository.save(failure);

        log.warn(
                "Failed notification persisted for order {}",
                context.orderId()
        );
    }
}
