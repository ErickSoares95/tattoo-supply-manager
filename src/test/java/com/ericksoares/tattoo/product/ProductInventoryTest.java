package com.ericksoares.tattoo.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductInventoryTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductInventory productInventory;

    private Product product(int stock) {
        return Product.builder()
                .id(1L)
                .name("Máquina Pen")
                .price(BigDecimal.valueOf(150))
                .stock(stock)
                .build();
    }

    @Test
    void shouldDecreaseStockAndReturnReservedProduct() {

        Product product = product(10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ReservedProduct reserved = productInventory.reserve(1L, 3);

        assertEquals(7, product.getStock());
        assertEquals(new ReservedProduct(1L, "Máquina Pen", BigDecimal.valueOf(150)), reserved);
        verify(productRepository).save(product);
    }

    @Test
    void shouldThrowWhenProductNotFound() {

        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productInventory.reserve(999L, 1));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldThrowAndKeepStockWhenStockIsInsufficient() {

        Product product = product(2);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(InsufficientStockException.class, () -> productInventory.reserve(1L, 3));
        assertEquals(2, product.getStock());
        verify(productRepository, never()).save(any());
    }
}
