package com.ericksoares.tattoo.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteProductServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private DeleteProductService service;

    @Test
    void shouldDeleteProductSuccessfully() {

        Product product = Product.builder().build();

        when(repository.findById(1L))
                .thenReturn(Optional.of(product));

        service.execute(1L);

        verify(repository).delete(product);
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
