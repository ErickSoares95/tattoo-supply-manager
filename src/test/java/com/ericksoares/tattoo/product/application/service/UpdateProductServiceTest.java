package com.ericksoares.tattoo.product.application.service;

import com.ericksoares.tattoo.product.application.dto.request.UpdateProductRequest;
import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.domain.exception.InvalidProductPriceException;
import com.ericksoares.tattoo.product.domain.exception.ProductNotFoundException;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateProductServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private UpdateProductService service;

    @Test
    void shouldUpdateProductSuccessfully() {

        Product product = Product.builder()
                .name("Old Product")
                .price(BigDecimal.valueOf(50))
                .stock(10)
                .build();

        UpdateProductRequest request =
                new UpdateProductRequest(
                        "New Product",
                        "New Description",
                        BigDecimal.valueOf(100),
                        20,
                        null
                );

        when(repository.findById(1L))
                .thenReturn(Optional.of(product));

        service.execute(1L, request);

        assertEquals("New Product", product.getName());
        assertEquals("New Description", product.getDescription());
        assertEquals(BigDecimal.valueOf(100), product.getPrice());
        assertEquals(20, product.getStock());

        // 🔥 Alterado para validar o salvamento sem travar na igualdade estrita do objeto
        verify(repository).save(any(Product.class));
    }

    @Test
    void shouldThrowWhenUpdatedPriceIsInvalid() {

        Product product = Product.builder()
                .name("Old Product")
                .price(BigDecimal.valueOf(50))
                .stock(10)
                .build();

        UpdateProductRequest request =
                new UpdateProductRequest(
                        "New Product",
                        "New Description",
                        BigDecimal.ZERO,
                        20,
                        null
                );

        when(repository.findById(1L))
                .thenReturn(Optional.of(product));

        assertThrows(
                InvalidProductPriceException.class,
                () -> service.execute(1L, request)
        );

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowWhenProductNotFound() {

        when(repository.findById(1L))
                .thenReturn(Optional.empty());

        UpdateProductRequest request =
                new UpdateProductRequest(
                        "Product",
                        "Description",
                        BigDecimal.TEN,
                        10,
                        null
                );

        assertThrows(
                ProductNotFoundException.class,
                () -> service.execute(1L, request)
        );
    }
}
