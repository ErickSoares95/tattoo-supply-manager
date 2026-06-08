package com.ericksoares.tattoo.product.application.service;

import com.ericksoares.tattoo.product.application.dto.request.ProductFilterRequest;
import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import com.ericksoares.tattoo.product.infrastructure.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindProductsService {

    private final ProductRepository repository;

    public Page<Product> execute(ProductFilterRequest filter, Pageable pageable) {

        Specification<Product> spec = Specification.where(
                ProductSpecification.nameContains(filter.name())
        ).and(
                ProductSpecification.descriptionContains(filter.description())
        ).and(
                ProductSpecification.priceGreaterThanOrEqual(filter.minPrice())
        ).and(
                ProductSpecification.priceLessThanOrEqual(filter.maxPrice())
        ).and(
                ProductSpecification.stockGreaterThanOrEqual(filter.minStock())
        );

        return repository.findAll(spec, pageable);
    }
}
