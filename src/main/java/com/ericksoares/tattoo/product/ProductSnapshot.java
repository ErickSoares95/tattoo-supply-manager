package com.ericksoares.tattoo.product;

import java.math.BigDecimal;

/** Read-only view of a product for other modules (see {@code ProductCatalog}). */
public record ProductSnapshot(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock
) {}
