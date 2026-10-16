package com.ericksoares.tattoo.order.application.dto;

import org.springframework.modulith.NamedInterface;

import java.math.BigDecimal;

/** What other modules (payment) need to know about an order. */
@NamedInterface("api")
public record OrderSummary(
        Long id,
        Long userId,
        BigDecimal total
) {}
