package com.ericksoares.tattoo.order.application.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        Integer quantity,
        BigDecimal price,
        BigDecimal subtotal
) {}
