package com.ericksoares.tattoo.order.application.dto;

public record OrderItemRequest(
        Long productId,
        Integer quantity
) {}
