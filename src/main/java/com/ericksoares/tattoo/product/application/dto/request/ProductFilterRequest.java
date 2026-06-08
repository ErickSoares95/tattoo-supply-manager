package com.ericksoares.tattoo.product.application.dto.request;

import java.math.BigDecimal;

public record ProductFilterRequest(
        String name,
        String description,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minStock
) {}
