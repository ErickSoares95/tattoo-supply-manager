package com.ericksoares.tattoo.ai.application.dto;

import java.util.List;

public record RestockRecommendationResponse(
        String recommendation,
        List<ProductSalesSummary> basedOn
) {}
