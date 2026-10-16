package com.ericksoares.tattoo.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface ProductRepository extends
        JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product>
{
    // Intentional table-level coupling to the order module's order_items table (same idea as
    // report's product_sales_report VIEW): the order module depends on product (it reserves
    // stock through ProductInventory), so reading sales through order's code would create a
    // product <-> order cycle. Each row is {product_id, units sold}.
    @Query(value = """
            SELECT oi.product_id, SUM(oi.quantity)
            FROM order_items oi
            GROUP BY oi.product_id
            """, nativeQuery = true)
    List<Object[]> sumUnitsSoldByProductId();
}
