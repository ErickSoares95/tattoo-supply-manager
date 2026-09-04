package com.ericksoares.tattoo.product.application.service;

import com.ericksoares.tattoo.product.application.dto.response.ProductResponse;
import com.ericksoares.tattoo.product.application.mapper.ProductMapper;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.ericksoares.tattoo.product.application.dto.request.ProductFilterRequest;
import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.infrastructure.specification.ProductSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindAllProductsService {

    // Sort key the storefront sends for "mais vendido primeiro" (GET /products?sort=
    // unitsSold,desc) - not a real column on Product, so it can't be pushed down to the
    // database the way price/name sorting is. Handled separately below.
    private static final String UNITS_SOLD_SORT_PROPERTY = "unitsSold";

    private final ProductRepository repository;
    private final ProductSalesLookup salesLookup;

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
        ).and(
                ProductSpecification.hasCategory(filter.category())
        );

        Map<Long, Long> salesByProductId = salesLookup.loadUnitsSoldByProductId();

        var unitsSoldOrder = pageable.getSort().getOrderFor(UNITS_SOLD_SORT_PROPERTY);

        if (unitsSoldOrder != null) {
            // Catalog is small enough (single digits of products today) that fetching
            // everything matching the filter and sorting/paginating in memory is fine -
            // a real "ORDER BY units_sold" would need a SQL-level join with order_items,
            // which is more machinery than this dataset size justifies right now.
            return sortByUnitsSoldInMemory(spec, pageable, salesByProductId, unitsSoldOrder.isDescending());
        }

        return repository.findAll(spec, pageable)
                .map(product -> ProductMapper.toResponse(product, salesLookup.unitsSoldFor(product.getId(), salesByProductId)));
    }

    private Page<ProductResponse> sortByUnitsSoldInMemory(
            Specification<Product> spec,
            Pageable pageable,
            Map<Long, Long> salesByProductId,
            boolean descending
    ) {
        List<ProductResponse> all = repository.findAll(spec).stream()
                .map(product -> ProductMapper.toResponse(product, salesLookup.unitsSoldFor(product.getId(), salesByProductId)))
                .sorted(descending
                        ? Comparator.comparingLong(ProductResponse::unitsSold).reversed()
                        : Comparator.comparingLong(ProductResponse::unitsSold))
                .toList();

        int start = (int) pageable.getOffset();
        if (start >= all.size()) {
            return new PageImpl<>(List.of(), pageable, all.size());
        }
        int end = Math.min(start + pageable.getPageSize(), all.size());

        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }
}
