package com.ericksoares.tattoo.product.application.service;

import com.ericksoares.tattoo.product.application.dto.ProductResponse;
import com.ericksoares.tattoo.product.application.mapper.ProductMapper;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ericksoares.tattoo.product.application.dto.ProductFilterRequest;
import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.infrastructure.specification.ProductSpecification;
import org.springframework.data.jpa.domain.Specification;

@Service
public class FindAllProductsService {

    private final ProductRepository repository;

    public FindAllProductsService(ProductRepository repository) {
        this.repository = repository;
    }

    public Page<ProductResponse> execute(
            ProductFilterRequest filter,
            Pageable pageable
    ) {

        Specification<Product> spec = Specification.where(
                ProductSpecification.nameContains(filter.name())
        ).and(
                ProductSpecification.priceGreaterThanOrEqual(filter.minPrice())
        ).and(
                ProductSpecification.priceLessThanOrEqual(filter.maxPrice())
        ).and(
                ProductSpecification.stockGreaterThanOrEqual(filter.minStock())
        );

        return repository.findAll(spec, pageable)
                .map(ProductMapper::toResponse);
    }
}
