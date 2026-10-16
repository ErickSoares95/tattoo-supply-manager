package com.ericksoares.tattoo.product.application.dto.response;

import org.springframework.modulith.NamedInterface;

import java.math.BigDecimal;

/** Read-only view of a product for other modules (see {@code ProductCatalog}). */
@NamedInterface("api")
public record ProductSnapshot(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock
) {}
