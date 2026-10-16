package com.ericksoares.tattoo.ai;

record ProductSalesSummary(
        Long productId,
        String productName,
        Long totalSold,
        Integer currentStock
) {}
