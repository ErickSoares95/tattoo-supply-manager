package com.ericksoares.tattoo.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

record OrderResponse(
        Long id,
        Long userId,
        List<OrderItemResponse> items,
        BigDecimal total,
        LocalDateTime creationDate
) {}
