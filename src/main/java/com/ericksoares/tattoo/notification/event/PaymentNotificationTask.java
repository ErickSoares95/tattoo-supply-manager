package com.ericksoares.tattoo.notification.event;

import com.ericksoares.tattoo.notification.application.dto.SenderFailure;
import com.ericksoares.tattoo.notification.application.service.FailedPaymentNotificationRecorder;
import com.ericksoares.tattoo.notification.application.service.NotificationService;
import com.ericksoares.tattoo.payment.domain.event.PaymentProcessedEvent;
import com.ericksoares.tattoo.shared.config.RejectionAwareTask;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Sends the "payment confirmed/rejected" notification for one payment event. If the
 * notification executor rejects it, {@link #onRejected()} persists one pending failure per
 * sender (due right away) so the scheduled retry delivers it instead of the event being lost.
 */
@Slf4j
class PaymentNotificationTask implements RejectionAwareTask {

    private final PaymentProcessedEvent event;
    private final NotificationService notificationService;
    private final FailedPaymentNotificationRecorder failedRecorder;

    PaymentNotificationTask(
            PaymentProcessedEvent event,
            NotificationService notificationService,
            FailedPaymentNotificationRecorder failedRecorder
    ) {
        this.event = event;
        this.notificationService = notificationService;
        this.failedRecorder = failedRecorder;
    }

    @Override
    public void run() {

        log.info("Processing in thread: {}", Thread.currentThread().getName());

        try {
            List<SenderFailure> failures = switch (event.status()) {
                case APPROVED -> notificationService.notifyPaymentConfirmed(event.orderId());
                case REJECTED -> notificationService.notifyPaymentRejected(event.orderId());
                case PENDING -> {
                    log.warn("PaymentProcessedEvent with unexpected status PENDING for order {}", event.orderId());
                    yield List.of();
                }
            };

            // One failure row per (event, sender), so a replay only touches the channel that failed.
            for (SenderFailure failure : failures) {
                failedRecorder.recordFailure(
                        event.eventId(), event.orderId(), event.status().name(), event.amount(), failure);

                log.warn(
                        "Failed payment notification persisted for order {} (sender {})",
                        event.orderId(),
                        failure.senderName()
                );
            }

        } catch (Exception e) {
            log.error("Failed to process payment notification for order {}", event.orderId(), e);
        }
    }

    @Override
    public void onRejected() {
        try {
            for (String senderName : notificationService.senderNames()) {
                failedRecorder.recordRejected(
                        event.eventId(), event.orderId(), event.status().name(), event.amount(), senderName);
            }

            log.warn("Payment notification task for order {} rejected by the executor; persisted for scheduled retry",
                    event.orderId());

        } catch (Exception e) {
            log.error("Could not persist rejected payment notification for order {} - event lost",
                    event.orderId(), e);
        }
    }
}
