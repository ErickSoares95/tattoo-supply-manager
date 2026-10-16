package com.ericksoares.tattoo.product;

import java.math.BigDecimal;

record ProductFilterRequest(
        String name,
        String description,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minStock,
        ProductCategory category,
        // Not a real Product column (Product.isOnDailyDeal is computed from
        // creationDate) - can't be pushed down via ProductSpecification like the other
        // filters here, so FindAllProductsService handles it in memory alongside the
        // unitsSold sort special-case. null/false both mean "don't filter by it".
        Boolean onDeal
) {}
