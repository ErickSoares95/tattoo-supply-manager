package com.ericksoares.tattoo.product;

import java.math.BigDecimal;

public record ProductUpdatedEvent(
        Long productId,
        String name,
        BigDecimal price
) {}
