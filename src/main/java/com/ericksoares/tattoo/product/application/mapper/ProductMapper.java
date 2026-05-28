package com.ericksoares.tattoo.product.application.mapper;

import com.ericksoares.tattoo.product.application.dto.ProductRequest;
import com.ericksoares.tattoo.product.application.dto.ProductResponse;
import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.user.domain.entity.User;

public class ProductMapper {

    public static Product toEntity(ProductRequest request) {
        return Product.builder()
                        .name(request.name())
                        .price(request.price())
                        .stock(request.stock())
                        .build();
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
