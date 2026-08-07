package com.ericksoares.tattoo.ai.application.dto;

public record ProductSalesSummary(
        Long productId,
        String productName,
        Long totalSold,
        Integer currentStock
) {}
