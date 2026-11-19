package com.ericksoares.tattoo.notification.sender;

/**
 * One notification channel. Contract for real implementations (N13): every call must give up
 * within {@code notification.sender.timeout} (default 30s; use it as the client's connect+read
 * timeout) - a resend holds only a 2-minute lease on its row, see NotificationTimingValidator.
 * Let failures propagate; NotificationErrorClassifier decides which ones are retried.
 */
public interface NotificationSender {

    /**
     * Stable channel name, persisted in {@code failed_notifications.sender_name} so a failed
     * delivery can be retried on this channel only. Must not change once deployed.
     */
    String name();

    void sendOrderRegistered(Long orderId);
    void sendPaymentConfirmed(Long orderId);
    void sendPaymentRejected(Long orderId);
}
