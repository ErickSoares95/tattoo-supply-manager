package com.ericksoares.tattoo.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindAllProductsServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private FindAllProductsService service;

    private static ProductFilterRequest noFilter() {
        return new ProductFilterRequest(null, null, null, null, null, null, null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldLetTheDatabaseSortAndPaginateWithIdAsTieBreaker() {

        Product product = Product.builder().id(1L).name("Ink").price(BigDecimal.TEN).stock(5).unitsSold(7).build();
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        Page<ProductResponse> page = service.execute(noFilter(),
                PageRequest.of(2, 20, Sort.by(Sort.Direction.DESC, "unitsSold")));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), pageable.capture());
        assertEquals(2, pageable.getValue().getPageNumber());
        assertEquals(Sort.by(Sort.Direction.DESC, "unitsSold").and(Sort.by("id")), pageable.getValue().getSort());
        assertEquals(7, page.getContent().get(0).unitsSold());
    }
}
