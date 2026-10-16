package com.ericksoares.tattoo.product;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

// Reads units sold straight from the order_items table (native query in ProductRepository)
// instead of going through the order module: order already depends on product, so calling
// back into order would create a module cycle. The table coupling is intentional, same as
// report's product_sales_report VIEW.
//
// Deliberately separate from report.ProductSalesReport: that VIEW only
// counts order_items backed by an APPROVED payment (the financial view). "Mais vendido"
// counts every order item regardless of payment status, so unpaid or rejected orders
// still move a product up the catalog - a popularity signal, not revenue.
@Component
@RequiredArgsConstructor
class ProductSalesLookup {

    private final ProductRepository productRepository;

    public Map<Long, Long> loadUnitsSoldByProductId() {
        Map<Long, Long> unitsSold = new HashMap<>();
        for (Object[] row : productRepository.sumUnitsSoldByProductId()) {
            Long productId = row[0] == null ? null : ((Number) row[0]).longValue();
            unitsSold.put(productId, ((Number) row[1]).longValue());
        }
        return unitsSold;
    }

    public long unitsSoldFor(Long productId, Map<Long, Long> salesByProductId) {
        return salesByProductId.getOrDefault(productId, 0L);
    }
}
