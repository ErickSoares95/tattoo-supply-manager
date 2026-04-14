package com.ericksoares.tattoo.product.mapper;

import com.ericksoares.tattoo.product.dto.ProductRequest;
import com.ericksoares.tattoo.product.dto.ProductResponse;
import com.ericksoares.tattoo.product.entity.Product;

public class ProductMapper {

    public static Product toEntity(ProductRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setPrice(request.price());
        product.setStock(request.stock());
        return product;
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock()
        );
    }
}
