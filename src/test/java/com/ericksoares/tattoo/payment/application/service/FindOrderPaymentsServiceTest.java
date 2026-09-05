package com.ericksoares.tattoo.payment.application.service;

import com.ericksoares.tattoo.order.domain.entity.Order;
import com.ericksoares.tattoo.order.domain.exception.OrderNotFoundException;
import com.ericksoares.tattoo.order.infrasctruture.repository.OrderRepository;
import com.ericksoares.tattoo.payment.application.dto.PaymentResponse;
import com.ericksoares.tattoo.payment.domain.entity.Payment;
import com.ericksoares.tattoo.payment.domain.entity.PaymentStatus;
import com.ericksoares.tattoo.payment.infrastructure.repository.PaymentRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindOrderPaymentsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private FindOrderPaymentsService service;

    @Test
    void shouldReturnPaymentsForOrderOwner() {

        Order order = Order.builder().id(1L).userId(10L).total(BigDecimal.TEN).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderIdOrderByCreationDateDesc(1L))
                .thenReturn(List.of(Payment.builder()
                        .id(5L).orderId(1L).amount(BigDecimal.TEN).method("PIX")
                        .status(PaymentStatus.APPROVED).build()));

        List<PaymentResponse> result = service.execute(1L, 10L, false);

        assertEquals(1, result.size());
        assertEquals(PaymentStatus.APPROVED, result.get(0).status());
    }

    @Test
    void shouldThrowWhenOrderNotFound() {
        when(orderRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(OrderNotFoundException.class, () -> service.execute(9L, 10L, false));
    }

    @Test
    void shouldThrowAccessDeniedWhenNotOwnerNorAdmin() {

        Order order = Order.builder().id(1L).userId(10L).total(BigDecimal.TEN).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class, () -> service.execute(1L, 999L, false));
        verify(paymentRepository, never()).findByOrderIdOrderByCreationDateDesc(any());
    }

    @Test
    void shouldAllowAdminToViewAnyOrderPayments() {

        Order order = Order.builder().id(1L).userId(10L).total(BigDecimal.TEN).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderIdOrderByCreationDateDesc(1L)).thenReturn(List.of());

        assertTrue(service.execute(1L, 999L, true).isEmpty());
    }
}
