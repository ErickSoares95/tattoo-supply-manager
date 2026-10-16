package com.ericksoares.tattoo.payment;

import com.ericksoares.tattoo.order.OrderSummary;
import com.ericksoares.tattoo.order.OrderNotFoundException;
import com.ericksoares.tattoo.order.OrderLookup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindOrderPaymentsServiceTest {

    @Mock
    private OrderLookup orderLookup;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private FindOrderPaymentsService service;

    @Test
    void shouldReturnPaymentsForOrderOwner() {

        OrderSummary order = new OrderSummary(1L, 10L, BigDecimal.TEN);
        when(orderLookup.findSummary(1L)).thenReturn(order);
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
        when(orderLookup.findSummary(9L)).thenThrow(new OrderNotFoundException(9L));
        assertThrows(OrderNotFoundException.class, () -> service.execute(9L, 10L, false));
    }

    @Test
    void shouldThrowAccessDeniedWhenNotOwnerNorAdmin() {

        OrderSummary order = new OrderSummary(1L, 10L, BigDecimal.TEN);
        when(orderLookup.findSummary(1L)).thenReturn(order);

        assertThrows(AccessDeniedException.class, () -> service.execute(1L, 999L, false));
        verify(paymentRepository, never()).findByOrderIdOrderByCreationDateDesc(any());
    }

    @Test
    void shouldAllowAdminToViewAnyOrderPayments() {

        OrderSummary order = new OrderSummary(1L, 10L, BigDecimal.TEN);
        when(orderLookup.findSummary(1L)).thenReturn(order);
        when(paymentRepository.findByOrderIdOrderByCreationDateDesc(1L)).thenReturn(List.of());

        assertTrue(service.execute(1L, 999L, true).isEmpty());
    }
}
