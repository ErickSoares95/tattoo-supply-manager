package com.ericksoares.tattoo.order;

import java.math.BigDecimal;

record OrderItemResponse(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal price,
        BigDecimal subtotal
) {}
