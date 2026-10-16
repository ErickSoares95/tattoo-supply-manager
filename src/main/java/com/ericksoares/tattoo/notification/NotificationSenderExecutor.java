package com.ericksoares.tattoo.notification;

import com.ericksoares.tattoo.notification.sender.NotificationSender;
import org.springframework.stereotype.Component;

/**
 * Single delivery attempt per call. There is deliberately no in-thread retry/backoff here:
 * sleeping between attempts would hold a notification-pool thread; a failed delivery is
 * instead persisted by the caller and retried by the scheduled job with exponential backoff
 * (see NotificationRetryJob / NotificationRetryPolicy).
 *
 * Failures leave here classified as TransientNotificationException (worth retrying) or
 * PermanentNotificationException (parked for manual review) - see NotificationErrorClassifier.
 * The classification lives here, not in each sender, so every channel gets it.
 */
@Component
class NotificationSenderExecutor {

    public void send(NotificationSender sender, Long orderId) {
        try {
            sender.sendOrderRegistered(orderId);
        } catch (Exception e) {
            throw NotificationErrorClassifier.classify(e);
        }
    }

    public void sendPaymentConfirmed(NotificationSender sender, Long orderId) {
        try {
            sender.sendPaymentConfirmed(orderId);
        } catch (Exception e) {
            throw NotificationErrorClassifier.classify(e);
        }
    }

    public void sendPaymentRejected(NotificationSender sender, Long orderId) {
        try {
            sender.sendPaymentRejected(orderId);
        } catch (Exception e) {
            throw NotificationErrorClassifier.classify(e);
        }
    }
}
