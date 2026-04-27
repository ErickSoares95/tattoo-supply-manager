package com.ericksoares.tattoo.notification.application.listener;

public interface NotificationSender {
    void sendOrderRegistered(Long orderId);
}
