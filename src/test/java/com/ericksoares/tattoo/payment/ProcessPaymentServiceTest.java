package com.ericksoares.tattoo.payment;

import com.ericksoares.tattoo.order.OrderSummary;
import com.ericksoares.tattoo.order.OrderNotFoundException;
import com.ericksoares.tattoo.order.OrderLookup;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentServiceTest {

    @Mock
    private OrderLookup orderLookup;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProcessPaymentService service;

    @Test
    void shouldApprovePaymentWhenAmountMatchesOrderTotal() {

        OrderSummary order = new OrderSummary(1L, 10L, BigDecimal.valueOf(100));

        PaymentRequest request = new PaymentRequest(BigDecimal.valueOf(100), "PIX");

        when(orderLookup.findSummary(1L)).thenReturn(order);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.execute(1L, request, 10L, false);

        assertEquals(PaymentStatus.APPROVED, response.status());

        verify(paymentRepository)
                .save(any(Payment.class));

        verify(eventPublisher).publishEvent(
                argThat((Object payload) -> payload instanceof PaymentProcessedEvent event
                        && event.status() == PaymentStatus.APPROVED
                        && event.orderId().equals(1L)));
    }

    @Test
    void shouldRejectPaymentWhenAmountDivergesFromOrderTotal() {

        OrderSummary order = new OrderSummary(1L, 10L, BigDecimal.valueOf(100));

        PaymentRequest request = new PaymentRequest(BigDecimal.valueOf(50), "PIX");

        when(orderLookup.findSummary(1L)).thenReturn(order);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.execute(1L, request, 10L, false);

        assertEquals(PaymentStatus.REJECTED, response.status());

        verify(eventPublisher).publishEvent(
                argThat((Object payload) -> payload instanceof PaymentProcessedEvent event
                        && event.status() == PaymentStatus.REJECTED
                        && event.orderId().equals(1L)));
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {

        PaymentRequest request = new PaymentRequest(BigDecimal.valueOf(100), "PIX");

        when(orderLookup.findSummary(999L)).thenThrow(new OrderNotFoundException(999L));

        assertThrows(
                OrderNotFoundException.class,
                () -> service.execute(999L, request, 10L, false)
        );

        verify(paymentRepository, never())
                .save(any());

        verify(eventPublisher, never())
                .publishEvent(any(Object.class));
    }

    @Test
    void shouldThrowExceptionWhenOrderAlreadyHasApprovedPayment() {

        OrderSummary order = new OrderSummary(1L, 10L, BigDecimal.valueOf(100));

        PaymentRequest request = new PaymentRequest(BigDecimal.valueOf(100), "PIX");

        when(orderLookup.findSummary(1L)).thenReturn(order);

        when(paymentRepository.existsByOrderIdAndStatus(1L, PaymentStatus.APPROVED))
                .thenReturn(true);

        assertThrows(
                OrderAlreadyPaidException.class,
                () -> service.execute(1L, request, 10L, false)
        );

        verify(paymentRepository, never())
                .save(any());

        verify(eventPublisher, never())
                .publishEvent(any(Object.class));
    }

    @Test
    void shouldThrowAccessDeniedWhenUserIsNotOwnerNorAdmin() {

        OrderSummary order = new OrderSummary(1L, 10L, BigDecimal.valueOf(100));

        PaymentRequest request = new PaymentRequest(BigDecimal.valueOf(100), "PIX");

        when(orderLookup.findSummary(1L)).thenReturn(order);

        assertThrows(
                AccessDeniedException.class,
                () -> service.execute(1L, request, 999L, false)
        );

        verify(paymentRepository, never())
                .save(any());

        verify(eventPublisher, never())
                .publishEvent(any(Object.class));
    }

    @Test
    void shouldAllowAdminToPayForAnyOrder() {

        OrderSummary order = new OrderSummary(1L, 10L, BigDecimal.valueOf(100));

        PaymentRequest request = new PaymentRequest(BigDecimal.valueOf(100), "PIX");

        when(orderLookup.findSummary(1L)).thenReturn(order);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.execute(1L, request, 999L, true);

        assertEquals(PaymentStatus.APPROVED, response.status());
    }
}
