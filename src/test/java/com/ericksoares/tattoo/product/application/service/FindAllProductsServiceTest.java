package com.ericksoares.tattoo.product.application.service;

import com.ericksoares.tattoo.product.application.dto.request.ProductFilterRequest;
import com.ericksoares.tattoo.product.application.dto.response.ProductResponse;
import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindAllProductsServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private ProductSalesLookup salesLookup;

    @InjectMocks
    private FindAllProductsService service;

    private Product productWithId(Long id, String name) {
        Product product = Product.builder().name(name).price(BigDecimal.TEN).stock(5).build();
        product.setId(id);
        return product;
    }

    private Product productOnDeal(Long id, String name) {
        Product product = productWithId(id, name);
        product.setCreationDate(LocalDateTime.now().minusMonths(4));
        return product;
    }

    // sort=unitsSold,desc can't be delegated to the database (units sold isn't a Product
    // column, see FindAllProductsService's comment) - this confirms the in-memory
    // fallback actually reorders by real sales instead of silently ignoring the sort.
    @Test
    void shouldSortByUnitsSoldDescendingWhenRequested() {

        Product lowSeller = productWithId(1L, "Low seller");
        Product bestSeller = productWithId(2L, "Best seller");

        when(repository.findAll(any(Specification.class)))
                .thenReturn(List.of(lowSeller, bestSeller));
        when(salesLookup.loadUnitsSoldByProductId())
                .thenReturn(Map.of(1L, 3L, 2L, 50L));
        when(salesLookup.unitsSoldFor(1L, Map.of(1L, 3L, 2L, 50L))).thenReturn(3L);
        when(salesLookup.unitsSoldFor(2L, Map.of(1L, 3L, 2L, 50L))).thenReturn(50L);

        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("unitsSold")));
        var page = service.execute(new ProductFilterRequest(null, null, null, null, null, null, null), pageable);

        assertEquals(2, page.getContent().size());
        assertEquals("Best seller", page.getContent().get(0).name());
        assertEquals("Low seller", page.getContent().get(1).name());
    }

    @Test
    void shouldPaginateInMemoryWhenSortingByUnitsSold() {

        Product first = productWithId(1L, "First");
        Product second = productWithId(2L, "Second");
        Product third = productWithId(3L, "Third");

        when(repository.findAll(any(Specification.class)))
                .thenReturn(List.of(first, second, third));
        when(salesLookup.loadUnitsSoldByProductId()).thenReturn(Map.of());
        when(salesLookup.unitsSoldFor(any(), any())).thenReturn(0L);

        var pageable = PageRequest.of(1, 1, Sort.by(Sort.Order.desc("unitsSold")));
        var page = service.execute(new ProductFilterRequest(null, null, null, null, null, null, null), pageable);

        assertEquals(1, page.getContent().size());
        assertEquals(3, page.getTotalElements());
    }

    @Test
    void shouldDelegateToRepositoryPaginationWhenNotSortingByUnitsSold() {

        Product product = productWithId(1L, "Ink");

        when(repository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(product)));
        when(salesLookup.loadUnitsSoldByProductId()).thenReturn(Map.of());
        when(salesLookup.unitsSoldFor(any(), any())).thenReturn(0L);

        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("price")));
        var page = service.execute(new ProductFilterRequest(null, null, null, null, null, null, null), pageable);

        List<ProductResponse> content = page.getContent();
        assertEquals(1, content.size());
        assertEquals("Ink", content.get(0).name());
    }

    // onDeal isn't a real Product column either (Product.isOnDailyDeal is computed from
    // creationDate) - same in-memory fallback as the unitsSold sort, this confirms
    // filter.onDeal()=true actually excludes products that aren't eligible.
    @Test
    void shouldFilterToOnlyOnDealProductsWhenRequested() {

        Product regular = productWithId(1L, "Regular");
        Product onDeal = productOnDeal(2L, "On deal");

        when(repository.findAll(any(Specification.class)))
                .thenReturn(List.of(regular, onDeal));
        when(salesLookup.loadUnitsSoldByProductId()).thenReturn(Map.of());
        when(salesLookup.unitsSoldFor(any(), any())).thenReturn(0L);

        var pageable = PageRequest.of(0, 10, Sort.unsorted());
        var page = service.execute(
                new ProductFilterRequest(null, null, null, null, null, null, true),
                pageable
        );

        assertEquals(1, page.getContent().size());
        assertEquals("On deal", page.getContent().get(0).name());
    }
}
