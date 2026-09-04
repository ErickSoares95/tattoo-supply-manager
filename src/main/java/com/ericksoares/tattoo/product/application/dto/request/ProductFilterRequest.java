package com.ericksoares.tattoo.product.application.dto.request;

import com.ericksoares.tattoo.product.domain.enums.ProductCategory;

import java.math.BigDecimal;

public record ProductFilterRequest(
        String name,
        String description,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minStock,
        ProductCategory category
) {}
