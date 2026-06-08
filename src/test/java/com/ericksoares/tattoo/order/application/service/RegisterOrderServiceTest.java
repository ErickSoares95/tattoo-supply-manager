package com.ericksoares.tattoo.order.application.service;

import com.ericksoares.tattoo.order.application.dto.OrderItemRequest;
import com.ericksoares.tattoo.order.application.dto.OrderRequest;
import com.ericksoares.tattoo.order.domain.entity.Order;
import com.ericksoares.tattoo.order.infrasctruture.repository.OrderRepository;
import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.domain.exception.ProductNotFoundException;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterOrderServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private RegisterOrderService service;

    @Test
    void shouldRegisterOrderSuccessfully() {

        Product product = Product.builder()
                .id(1L)
                .name("Fonte Dragon")
                .price(BigDecimal.valueOf(100))
                .stock(10)
                .build();

        OrderRequest request = new OrderRequest(
                List.of(
                        new OrderItemRequest(
                                1L,
                                2
                        )
                )
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order order = service.execute(request);

        assertNotNull(order);

        verify(productRepository)
                .findById(1L);

        verify(orderRepository)
                .save(any(Order.class));

        verify(publisher)
                .publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenProductNotFound() {

        OrderRequest request = new OrderRequest(
                List.of(
                        new OrderItemRequest(
                                999L,
                                1
                        )
                )
        );

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> service.execute(request)
        );

        verify(orderRepository, never())
                .save(any());

        verify(publisher, never())
                .publishEvent(any());
    }

    @Test
    void shouldDecreaseProductStock() {

        Product product = Product.builder()
                .id(1L)
                .name("Máquina Pen")
                .price(BigDecimal.valueOf(150))
                .stock(10)
                .build();

        OrderRequest request = new OrderRequest(
                List.of(
                        new OrderItemRequest(
                                1L,
                                3
                        )
                )
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(request);

        assertEquals(
                7,
                product.getStock()
        );
    }
}
