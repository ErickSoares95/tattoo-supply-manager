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
                        .category(request.category())
                        .build();
    }

    // Admin create/update/delete responses don't need real sales data (a product just
    // created or edited has nothing new to report), so this overload defaults it to 0
    // instead of forcing every caller to look it up.
    public static ProductResponse toResponse(Product product) {
        return toResponse(product, 0L);
    }

    public static ProductResponse toResponse(Product product, long unitsSold) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getImageUrl(),
                product.getCategory(),
                product.isOnDailyDeal(),
                unitsSold
        );
    }
}
