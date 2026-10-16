package com.ericksoares.tattoo.ai;

import com.ericksoares.tattoo.order.ProductUnitsSold;
import com.ericksoares.tattoo.order.OrderLookup;
import com.ericksoares.tattoo.product.ProductSnapshot;
import com.ericksoares.tattoo.product.ProductCatalog;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
class RecommendRestockService {

    private final OrderLookup orderLookup;
    private final ProductCatalog productCatalog;
    private final LlmClient llmClient;

    public RecommendRestockService(
            OrderLookup orderLookup,
            ProductCatalog productCatalog,
            LlmClient llmClient
    ) {
        this.orderLookup = orderLookup;
        this.productCatalog = productCatalog;
        this.llmClient = llmClient;
    }

    public RestockRecommendationResponse execute() {

        List<ProductUnitsSold> salesSummary =
                orderLookup.findUnitsSoldByProduct();

        if (salesSummary.isEmpty()) {
            return new RestockRecommendationResponse(
                    "No sales history yet — not enough data for a restock recommendation.",
                    List.of()
            );
        }

        Map<Long, Integer> currentStockByProductId = productCatalog
                .findAllById(salesSummary.stream().map(ProductUnitsSold::productId).toList())
                .stream()
                .collect(Collectors.toMap(ProductSnapshot::id, ProductSnapshot::stock));

        List<ProductSalesSummary> basedOn = salesSummary.stream()
                .map(row -> new ProductSalesSummary(
                        row.productId(),
                        row.productName(),
                        row.totalSold(),
                        currentStockByProductId.getOrDefault(row.productId(), 0)
                ))
                .toList();

        String recommendation = llmClient.complete(buildPrompt(basedOn));

        return new RestockRecommendationResponse(recommendation, basedOn);
    }

    private String buildPrompt(List<ProductSalesSummary> summary) {

        String rows = summary.stream()
                .map(item -> "- %s: sold %d units total, %d units currently in stock"
                        .formatted(item.productName(), item.totalSold(), item.currentStock()))
                .collect(Collectors.joining("\n"));

        return """
                You are a restock advisor for a tattoo supply store.
                Based on the sales history and current stock below, recommend which products \
                need restocking soon and roughly how much, in a short bullet list. \
                Be concise and prioritize items with high sales relative to low remaining stock.

                %s
                """.formatted(rows);
    }
}
