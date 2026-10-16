package com.ericksoares.tattoo.notification.event;

import com.ericksoares.tattoo.notification.application.service.FailedPaymentNotificationRecorder;
import com.ericksoares.tattoo.notification.application.service.NotificationService;
import com.ericksoares.tattoo.payment.domain.event.PaymentProcessedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.Executor;

/**
 * Payment counterpart of {@link NotificationListener}. Runs AFTER_COMMIT so a rolled-back
 * payment never notifies, and is not {@code @Async} on purpose: the work is handed to the
 * executor as a {@link PaymentNotificationTask}, so a saturated pool persists the pending
 * notification (via the rejection handler) instead of dropping it.
 */
@Slf4j
@Component
public class PaymentNotificationListener {

    private final NotificationService notificationService;
    private final FailedPaymentNotificationRecorder failedRecorder;
    private final Executor notificationExecutor;

    public PaymentNotificationListener(
            NotificationService notificationService,
            FailedPaymentNotificationRecorder failedRecorder,
            @Qualifier("notificationExecutor") Executor notificationExecutor
    ) {
        this.notificationService = notificationService;
        this.failedRecorder = failedRecorder;
        this.notificationExecutor = notificationExecutor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PaymentProcessedEvent event) {

        log.info("Received PaymentProcessedEvent for order {} (status {})", event.orderId(), event.status());

        notificationExecutor.execute(
                new PaymentNotificationTask(event, notificationService, failedRecorder)
        );
    }
}
