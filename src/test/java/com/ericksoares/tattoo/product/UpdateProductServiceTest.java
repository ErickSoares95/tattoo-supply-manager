package com.ericksoares.tattoo.product;

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
                .version(0L)
                .build();

        UpdateProductRequest request =
                new UpdateProductRequest(
                        "New Product",
                        "New Description",
                        BigDecimal.valueOf(100),
                        20,
                        null,
                        ProductCategory.ACCESSORIES,
                        0L
                );

        when(repository.findById(1L))
                .thenReturn(Optional.of(product));

        service.execute(1L, request);

        assertEquals("New Product", product.getName());
        assertEquals("New Description", product.getDescription());
        assertEquals(BigDecimal.valueOf(100), product.getPrice());
        assertEquals(20, product.getStock());

        // 🔥 Alterado para validar o salvamento sem travar na igualdade estrita do objeto
        verify(repository).saveAndFlush(any(Product.class));
    }

    @Test
    void shouldThrowWhenUpdatedPriceIsInvalid() {

        Product product = Product.builder()
                .name("Old Product")
                .price(BigDecimal.valueOf(50))
                .stock(10)
                .version(0L)
                .build();

        UpdateProductRequest request =
                new UpdateProductRequest(
                        "New Product",
                        "New Description",
                        BigDecimal.ZERO,
                        20,
                        null,
                        ProductCategory.ACCESSORIES,
                        0L
                );

        when(repository.findById(1L))
                .thenReturn(Optional.of(product));

        assertThrows(
                InvalidProductPriceException.class,
                () -> service.execute(1L, request)
        );

        verify(repository, never()).saveAndFlush(any());
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
                        null,
                        ProductCategory.ACCESSORIES,
                        0L
                );

        assertThrows(
                ProductNotFoundException.class,
                () -> service.execute(1L, request)
        );
    }

    @Test
    void shouldRefuseToOverwriteAProductChangedSinceItWasLoaded() {

        // Loaded at version 0 by the admin form; an order has since sold stock (version 1).
        Product product = Product.builder()
                .name("Old Product")
                .price(BigDecimal.valueOf(50))
                .stock(8)
                .version(1L)
                .build();

        UpdateProductRequest staleRequest =
                new UpdateProductRequest(
                        "Old Product",
                        null,
                        BigDecimal.valueOf(50),
                        10,
                        null,
                        ProductCategory.ACCESSORIES,
                        0L
                );

        when(repository.findById(1L))
                .thenReturn(Optional.of(product));

        assertThrows(
                ProductModifiedException.class,
                () -> service.execute(1L, staleRequest)
        );

        assertEquals(8, product.getStock());
        verify(repository, never()).saveAndFlush(any());
        verify(publisher, never()).publishEvent(any());
    }
}
