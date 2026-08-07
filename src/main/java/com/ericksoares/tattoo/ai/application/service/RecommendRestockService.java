package com.ericksoares.tattoo.ai.application.service;

import com.ericksoares.tattoo.ai.application.dto.ProductSalesSummary;
import com.ericksoares.tattoo.ai.application.dto.RestockRecommendationResponse;
import com.ericksoares.tattoo.order.infrasctruture.repository.OrderItemRepository;
import com.ericksoares.tattoo.order.infrasctruture.repository.OrderItemRepository.ProductSalesProjection;
import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecommendRestockService {

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final LlmClient llmClient;

    public RecommendRestockService(
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            LlmClient llmClient
    ) {
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.llmClient = llmClient;
    }

    public RestockRecommendationResponse execute() {

        List<ProductSalesProjection> salesSummary =
                orderItemRepository.findProductSalesSummary();

        if (salesSummary.isEmpty()) {
            return new RestockRecommendationResponse(
                    "No sales history yet — not enough data for a restock recommendation.",
                    List.of()
            );
        }

        Map<Long, Integer> currentStockByProductId = productRepository
                .findAllById(salesSummary.stream().map(ProductSalesProjection::getProductId).toList())
                .stream()
                .collect(Collectors.toMap(Product::getId, Product::getStock));

        List<ProductSalesSummary> basedOn = salesSummary.stream()
                .map(row -> new ProductSalesSummary(
                        row.getProductId(),
                        row.getProductName(),
                        row.getTotalSold(),
                        currentStockByProductId.getOrDefault(row.getProductId(), 0)
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
