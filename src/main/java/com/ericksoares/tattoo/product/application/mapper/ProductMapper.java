package com.ericksoares.tattoo.product.application.mapper;

import com.ericksoares.tattoo.product.application.dto.request.ProductRequest;
import com.ericksoares.tattoo.product.application.dto.response.ProductResponse;
import com.ericksoares.tattoo.product.domain.entity.Product;

public class ProductMapper {

    public static Product toEntity(ProductRequest request) {
        return Product.builder()
                        .name(request.name())
                        .description(request.description())
                        .price(request.price())
                        .stock(request.stock())
                        .imageUrl(request.imageUrl())
                        .build();
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getImageUrl(),
                product.isOnDailyDeal()
        );
    }
}
