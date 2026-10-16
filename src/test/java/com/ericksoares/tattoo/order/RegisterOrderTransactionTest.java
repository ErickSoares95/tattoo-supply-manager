package com.ericksoares.tattoo.order;

import com.ericksoares.tattoo.product.ReservedProduct;
import com.ericksoares.tattoo.product.ProductInventory;
import com.ericksoares.tattoo.product.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterOrderTransactionTest {

    @Mock
    private ProductInventory productInventory;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private RegisterOrderTransaction service;

    @Test
    void shouldRegisterOrderSuccessfully() {

        OrderRequest request = new OrderRequest(
                List.of(
                        new OrderItemRequest(
                                1L,
                                2
                        )
                )
        );

        when(productInventory.reserve(1L, 2))
                .thenReturn(new ReservedProduct(1L, "Fonte Dragon", BigDecimal.valueOf(100)));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse order = service.execute(request, 1L);

        assertNotNull(order);

        assertEquals(1L, order.userId());

        assertEquals("Fonte Dragon", order.items().get(0).productName());

        verify(productInventory)
                .reserve(1L, 2);

        verify(orderRepository)
                .save(any(Order.class));

        verify(publisher)
                .publishEvent(any(Object.class));
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

        when(productInventory.reserve(999L, 1))
                .thenThrow(new ProductNotFoundException(999L));

        assertThrows(
                ProductNotFoundException.class,
                () -> service.execute(request, 1L)
        );

        verify(orderRepository, never())
                .save(any());

        verify(publisher, never())
                .publishEvent(any());
    }

    @Test
    void shouldReserveStockForEachItem() {

        OrderRequest request = new OrderRequest(
                List.of(
                        new OrderItemRequest(1L, 3),
                        new OrderItemRequest(2L, 1)
                )
        );

        when(productInventory.reserve(1L, 3))
                .thenReturn(new ReservedProduct(1L, "Máquina Pen", BigDecimal.valueOf(150)));
        when(productInventory.reserve(2L, 1))
                .thenReturn(new ReservedProduct(2L, "Tinta Black", BigDecimal.valueOf(50)));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse order = service.execute(request, 1L);

        assertEquals(0, BigDecimal.valueOf(500).compareTo(order.total()));

        verify(productInventory).reserve(1L, 3);
        verify(productInventory).reserve(2L, 1);
    }

    @Test
    void shouldThrowAndNotReserveStockWhenQuantityIsInvalid() {

        OrderRequest request = new OrderRequest(
                List.of(
                        new OrderItemRequest(
                                1L,
                                0
                        )
                )
        );

        assertThrows(
                InvalidOrderItemQuantityException.class,
                () -> service.execute(request, 1L)
        );

        verify(productInventory, never())
                .reserve(anyLong(), anyInt());

        verify(orderRepository, never())
                .save(any());

        verify(publisher, never())
                .publishEvent(any());
    }
}
