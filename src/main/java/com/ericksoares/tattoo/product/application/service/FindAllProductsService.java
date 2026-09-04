package com.ericksoares.tattoo.product.application.service;

import com.ericksoares.tattoo.product.application.dto.response.ProductResponse;
import com.ericksoares.tattoo.product.application.mapper.ProductMapper;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    // database the way price/name sorting is. Handled in memory below, alongside the
    // onDeal filter (also not a real column - see ProductFilterRequest.onDeal).
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
        boolean onDealOnly = Boolean.TRUE.equals(filter.onDeal());
        boolean sortsByUnitsSold = pageable.getSort().getOrderFor(UNITS_SOLD_SORT_PROPERTY) != null;

        if (onDealOnly || sortsByUnitsSold) {
            // Catalog is small enough (dozens of products today) that fetching
            // everything matching the DB-level filters and filtering/sorting/paginating
            // the rest in memory is fine - a real "WHERE on_deal" or "ORDER BY
            // units_sold" would need a SQL-level view/join, which is more machinery
            // than this dataset size justifies right now.
            List<ProductResponse> all = repository.findAll(spec).stream()
                    .map(product -> ProductMapper.toResponse(product, salesLookup.unitsSoldFor(product.getId(), salesByProductId)))
                    .filter(product -> !onDealOnly || product.onDeal())
                    .sorted(comparatorFor(pageable.getSort()))
                    .toList();

            return paginate(all, pageable);
        }

        return repository.findAll(spec, pageable)
                .map(product -> ProductMapper.toResponse(product, salesLookup.unitsSoldFor(product.getId(), salesByProductId)));
    }

    // Only reached once we're already resolving everything in memory (see above) -
    // covers the same sort keys Pageable would otherwise push to the database (price,
    // name) plus unitsSold, which never could be. Falls back to id order, just so
    // pagination is stable/deterministic rather than depending on the database's
    // unspecified natural order.
    private Comparator<ProductResponse> comparatorFor(Sort sort) {
        for (Sort.Order order : sort) {
            Comparator<ProductResponse> comparator = switch (order.getProperty()) {
                case "price" -> Comparator.comparing(ProductResponse::price);
                case "name" -> Comparator.comparing(ProductResponse::name, String.CASE_INSENSITIVE_ORDER);
                case UNITS_SOLD_SORT_PROPERTY -> Comparator.comparingLong(ProductResponse::unitsSold);
                default -> null;
            };
            if (comparator != null) {
                return order.isDescending() ? comparator.reversed() : comparator;
            }
        }
        return Comparator.comparing(ProductResponse::id);
    }

    private Page<ProductResponse> paginate(List<ProductResponse> all, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start >= all.size()) {
            return new PageImpl<>(List.of(), pageable, all.size());
        }
        int end = Math.min(start + pageable.getPageSize(), all.size());

        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }
}
