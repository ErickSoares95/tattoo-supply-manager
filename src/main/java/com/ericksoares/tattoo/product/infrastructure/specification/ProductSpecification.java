package com.ericksoares.tattoo.product.infrastructure.specification;

import com.ericksoares.tattoo.product.domain.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ProductSpecification {

    public static Specification<Product> nameContains(String name) {
        return (root, query, cb) ->
                name == null ? null :
                        cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Product> priceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, cb) ->
                minPrice == null ? null :
                        cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> priceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, cb) ->
                maxPrice == null ? null :
                        cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Product> stockGreaterThanOrEqual(Integer minStock) {
        return (root, query, cb) ->
                minStock == null ? null :
                        cb.greaterThanOrEqualTo(root.get("stock"), minStock);
    }
}
