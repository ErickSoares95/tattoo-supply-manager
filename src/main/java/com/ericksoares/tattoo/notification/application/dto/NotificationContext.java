package com.ericksoares.tattoo.notification.application.dto;

public record NotificationContext(
        Long orderId,
        String productName,
        Integer quantity
) {}
