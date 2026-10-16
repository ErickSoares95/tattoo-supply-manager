package com.ericksoares.tattoo.ai;

import java.util.List;

record RestockRecommendationResponse(
        String recommendation,
        List<ProductSalesSummary> basedOn
) {}
