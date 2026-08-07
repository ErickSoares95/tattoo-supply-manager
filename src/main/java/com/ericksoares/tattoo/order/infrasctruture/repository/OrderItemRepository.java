package com.ericksoares.tattoo.order.infrasctruture.repository;

import com.ericksoares.tattoo.order.domain.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
            SELECT oi.productId AS productId,
                   oi.productName AS productName,
                   SUM(oi.quantity) AS totalSold
            FROM OrderItem oi
            GROUP BY oi.productId, oi.productName
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<ProductSalesProjection> findProductSalesSummary();

    interface ProductSalesProjection {
        Long getProductId();
        String getProductName();
        Long getTotalSold();
    }
}
