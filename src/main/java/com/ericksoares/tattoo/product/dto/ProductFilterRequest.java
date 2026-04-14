package com.ericksoares.tattoo.product.dto;

import java.math.BigDecimal;

public record ProductFilterRequest(
        String name,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minStock
) {}
