package com.ericksoares.tattoo.product.domain.event;

import java.math.BigDecimal;

public record ProductUpdatedEvent(
        Long productId,
        String name,
        BigDecimal price
) {}
