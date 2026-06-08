package com.ericksoares.tattoo.product.application.service;

import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.domain.exception.ProductNotFoundException;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindProductByIdServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private FindProductByIdService service;

    @Test
    void shouldFindProductById() {

        Product product = Product.builder()
                .name("Ink")
                .price(BigDecimal.TEN)
                .stock(10)
                .build();

        when(repository.findById(1L))
                .thenReturn(Optional.of(product));

        var response = service.execute(1L);

        assertNotNull(response);
    }

    @Test
    void shouldThrowWhenProductNotFound() {

        when(repository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> service.execute(1L)
        );
    }
}
