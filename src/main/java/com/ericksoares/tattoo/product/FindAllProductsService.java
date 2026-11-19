package com.ericksoares.tattoo.product;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class FindAllProductsService {

    private final ProductRepository repository;

    // P7: "mais vendido" (sort=unitsSold,desc) and "ofertas" (onDeal) used to be resolved in
    // memory over the whole catalog plus a SUM over all of order_items on every listing. Both
    // are SQL now (Product.unitsSold is a column, onDeal a creationDate predicate), so the
    // database filters, sorts and paginates.
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
        ).and(
                ProductSpecification.descriptionContains(filter.description())
        ).and(
                ProductSpecification.onDailyDeal(filter.onDeal())
        );

        return repository.findAll(spec, withStableOrder(pageable))
                .map(ProductMapper::toResponse);
    }

    // Ties (same units sold, same price) would otherwise come back in an unspecified order
    // and a product could jump between pages; id breaks them.
    private static Pageable withStableOrder(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().and(Sort.by("id")));
    }
}
