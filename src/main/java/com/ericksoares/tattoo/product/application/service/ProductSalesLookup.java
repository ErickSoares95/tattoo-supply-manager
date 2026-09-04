package com.ericksoares.tattoo.product.application.service;

import com.ericksoares.tattoo.order.infrasctruture.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

// Read-only cross-module repository access, same precedent already set by
// ai.application.service.RecommendRestockService (which also injects order's
// OrderItemRepository directly) - reading another module's data for a derived/reporting
// value doesn't need the full ApplicationEventPublisher machinery reserved for
// side-effecting workflows (see CLAUDE.md's module boundary note).
//
// Deliberately separate from report.domain.entity.ProductSalesReport: that VIEW only
// counts order_items backed by an APPROVED payment, which the storefront's checkout
// doesn't create yet (no payment step wired in there - see docs/plano-carreira.md's
// deferred "pagamento real" phase). Counting every OrderItem regardless of payment
// status is what makes "mais vendido" actually reflect real storefront activity today.
@Component
@RequiredArgsConstructor
public class ProductSalesLookup {

    private final OrderItemRepository orderItemRepository;

    public Map<Long, Long> loadUnitsSoldByProductId() {
        return orderItemRepository.findProductSalesSummary().stream()
                .collect(Collectors.toMap(
                        OrderItemRepository.ProductSalesProjection::getProductId,
                        OrderItemRepository.ProductSalesProjection::getTotalSold
                ));
    }

    public long unitsSoldFor(Long productId, Map<Long, Long> salesByProductId) {
        return salesByProductId.getOrDefault(productId, 0L);
    }
}
