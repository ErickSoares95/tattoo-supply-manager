package com.ericksoares.tattoo.order.application.dto;

public record OrderItemData(
        Long productId,
        String productName,
        Integer quantity
) {}
