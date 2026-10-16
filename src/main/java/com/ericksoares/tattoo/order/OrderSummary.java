package com.ericksoares.tattoo.order;

import java.math.BigDecimal;

/** What other modules (payment) need to know about an order. */
public record OrderSummary(
        Long id,
        Long userId,
        BigDecimal total
) {}
