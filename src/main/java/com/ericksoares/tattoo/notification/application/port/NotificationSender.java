package com.ericksoares.tattoo.notification.application.port;

public interface NotificationSender {
    void sendOrderRegistered(Long orderId);
}
