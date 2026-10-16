package com.ericksoares.tattoo.notification;

import com.ericksoares.tattoo.shared.config.RejectionAwareTask;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Sends the "order registered" notification for one order. If the notification executor
 * rejects it, {@link #onRejected()} persists one pending failure per sender (due right away)
 * so the scheduled retry delivers it instead of the event being lost.
 */
@Slf4j
class OrderNotificationTask implements RejectionAwareTask {

    private final Long orderId;
    private final NotificationService notificationService;
    private final FailedNotificationRecorder failedRecorder;

    OrderNotificationTask(
            Long orderId,
            NotificationService notificationService,
            FailedNotificationRecorder failedRecorder
    ) {
        this.orderId = orderId;
        this.notificationService = notificationService;
        this.failedRecorder = failedRecorder;
    }

    @Override
    public void run() {

        log.info("Processing in thread: {}", Thread.currentThread().getName());

        try {
            // One notification per order, regardless of how many items it has.
            NotificationContext context = new NotificationContext(orderId);

            List<SenderFailure> failures = notificationService.notifyOrderRegistered(context);

            // One failure row per (order, sender), so a replay only touches the channel that failed.
            for (SenderFailure failure : failures) {
                failedRecorder.recordFailure(orderId, failure);

                log.warn(
                        "Failed notification persisted for order {} (sender {})",
                        orderId,
                        failure.senderName()
                );
            }

        } catch (Exception e) {
            log.error("Failed to process notification for order {}", orderId, e);
        }
    }

    @Override
    public void onRejected() {
        try {
            for (String senderName : notificationService.senderNames()) {
                failedRecorder.recordRejected(orderId, senderName);
            }

            log.warn("Notification task for order {} rejected by the executor; persisted for scheduled retry", orderId);

        } catch (Exception e) {
            log.error("Could not persist rejected notification for order {} - event lost", orderId, e);
        }
    }
}
