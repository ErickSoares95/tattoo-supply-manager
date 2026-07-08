package com.ericksoares.tattoo.order.application.service;

import com.ericksoares.tattoo.order.application.dto.OrderResponse;
import com.ericksoares.tattoo.order.domain.entity.Order;
import com.ericksoares.tattoo.order.domain.entity.OrderItem;
import com.ericksoares.tattoo.order.domain.exception.OrderNotFoundException;
import com.ericksoares.tattoo.order.infrasctruture.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindOrderByIdServiceTest {

    @Mock
    private OrderRepository repository;

    @InjectMocks
    private FindOrderByIdService service;

    private Order buildOrder(Long id, Long userId) {
        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setQuantity(1);
        item.setPrice(BigDecimal.TEN);

        Order order = new Order();
        order.setId(id);
        order.setUserId(userId);
        order.setItems(List.of(item));
        order.setTotal(BigDecimal.TEN);

        return order;
    }

    @Test
    void shouldReturnOrderWhenOwnerRequestsIt() {

        when(repository.findById(1L))
                .thenReturn(Optional.of(buildOrder(1L, 10L)));

        OrderResponse response = service.execute(1L, 10L, false);

        assertNotNull(response);
        assertEquals(10L, response.userId());
    }

    @Test
    void shouldReturnOrderWhenAdminRequestsSomeoneElsesOrder() {

        when(repository.findById(1L))
                .thenReturn(Optional.of(buildOrder(1L, 10L)));

        OrderResponse response = service.execute(1L, 999L, true);

        assertNotNull(response);
        assertEquals(10L, response.userId());
    }

    @Test
    void shouldThrowAccessDeniedWhenNonOwnerNonAdminRequestsIt() {

        when(repository.findById(1L))
                .thenReturn(Optional.of(buildOrder(1L, 10L)));

        assertThrows(
                AccessDeniedException.class,
                () -> service.execute(1L, 999L, false)
        );
    }

    @Test
    void shouldThrowWhenOrderNotFound() {

        when(repository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> service.execute(1L, 10L, false)
        );
    }
}
