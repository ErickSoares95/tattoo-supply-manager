package com.ericksoares.tattoo.order.application.service;

import com.ericksoares.tattoo.order.application.dto.OrderResponse;
import com.ericksoares.tattoo.order.domain.entity.Order;
import com.ericksoares.tattoo.order.domain.entity.OrderItem;
import com.ericksoares.tattoo.order.infrasctruture.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindOrdersServiceTest {

    @Mock
    private OrderRepository repository;

    @InjectMocks
    private FindOrdersService service;

    private Order buildOrder(Long userId) {
        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setQuantity(1);
        item.setPrice(BigDecimal.TEN);

        Order order = new Order();
        order.setId(1L);
        order.setUserId(userId);
        order.setItems(List.of(item));
        order.setTotal(BigDecimal.TEN);

        return order;
    }

    @Test
    void shouldReturnOnlyOwnOrdersWhenNotAdmin() {

        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findByUserId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildOrder(10L))));

        Page<OrderResponse> result = service.execute(10L, false, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(10L, result.getContent().get(0).userId());

        verify(repository).findByUserId(eq(10L), any(Pageable.class));
        verify(repository, never()).findAll(any(Pageable.class));
    }

    @Test
    void shouldReturnAllOrdersWhenAdmin() {

        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildOrder(10L), buildOrder(20L))));

        Page<OrderResponse> result = service.execute(999L, true, pageable);

        assertEquals(2, result.getTotalElements());

        verify(repository).findAll(any(Pageable.class));
        verify(repository, never()).findByUserId(any(), any());
    }
}
