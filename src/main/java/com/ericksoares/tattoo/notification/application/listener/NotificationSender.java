package com.ericksoares.tattoo.notification.application.listener;

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
