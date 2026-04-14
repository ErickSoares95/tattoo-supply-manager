package com.ericksoares.tattoo.product.service;

import com.ericksoares.tattoo.product.dto.ProductFilterRequest;
import com.ericksoares.tattoo.product.entity.Product;
import com.ericksoares.tattoo.product.repository.ProductRepository;
import com.ericksoares.tattoo.product.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindProductsService {

    private final ProductRepository repository;

    public Page<Product> execute(ProductFilterRequest filter, Pageable pageable) {

        Specification<Product> spec = Specification.where(
                ProductSpecification.nameContains(filter.name())
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
