package com.ericksoares.tattoo.notification;

import com.ericksoares.tattoo.order.OrderRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.Executor;

/**
 * Not annotated with {@code @Async} on purpose: the work is handed to the executor explicitly
 * as an {@link OrderNotificationTask}, so that when the pool is saturated the executor's
 * rejection handler (shared.config.AsyncConfig) can persist the pending notification instead
 * of dropping it with a TaskRejectedException.
 */
@Slf4j
@Component
class NotificationListener  {

    private final NotificationService notificationService;
    private final FailedNotificationRecorder failedRecorder;
    private final Executor notificationExecutor;

    public NotificationListener(
            NotificationService notificationService,
            FailedNotificationRecorder failedRecorder,
            @Qualifier("notificationExecutor") Executor notificationExecutor
    ) {
        this.notificationService = notificationService;
        this.failedRecorder = failedRecorder;
        this.notificationExecutor = notificationExecutor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderRegisteredEvent event) {

        log.info("Received OrderRegisteredEvent for order {}", event.orderId());

        notificationExecutor.execute(
                new OrderNotificationTask(event.orderId(), notificationService, failedRecorder)
        );
    }
}
