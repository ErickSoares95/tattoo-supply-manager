package com.ericksoares.tattoo.product;

import java.math.BigDecimal;

public record ProductRegisteredEvent(
        Long productId,
        String name,
        BigDecimal price
) {}