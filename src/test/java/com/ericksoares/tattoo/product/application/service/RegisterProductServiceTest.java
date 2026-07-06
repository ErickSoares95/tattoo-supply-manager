package com.ericksoares.tattoo.product.application.service;

import com.ericksoares.tattoo.product.application.dto.request.ProductRequest;
import com.ericksoares.tattoo.product.application.dto.response.ProductResponse;
import com.ericksoares.tattoo.product.application.mapper.ProductMapper;
import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher; // 🔥 Import adicionado

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class RegisterProductServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private RegisterProductService service;

    @Test
    void shouldRegisterProduct() {

        ProductRequest request =
                new ProductRequest(
                        "Fonte FK Irons",
                        "Fonte profissional",
                        new BigDecimal("250"),
                        10
                );

        Product product =
                ProductMapper.toEntity(request);

        when(repository.save(any(Product.class)))
                .thenReturn(product);

        ProductResponse response =
                service.execute(request);

        assertNotNull(response);

        verify(repository)
                .save(any(Product.class));

        verify(publisher, times(1)).publishEvent(any(Object.class));
    }
}