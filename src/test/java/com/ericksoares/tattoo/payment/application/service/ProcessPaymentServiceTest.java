package com.ericksoares.tattoo.payment.application.service;

import com.ericksoares.tattoo.order.domain.entity.Order;
import com.ericksoares.tattoo.order.domain.exception.OrderNotFoundException;
import com.ericksoares.tattoo.order.infrasctruture.repository.OrderRepository;
import com.ericksoares.tattoo.payment.application.dto.PaymentRequest;
import com.ericksoares.tattoo.payment.application.dto.PaymentResponse;
import com.ericksoares.tattoo.payment.domain.entity.Payment;
import com.ericksoares.tattoo.payment.domain.entity.PaymentStatus;
import com.ericksoares.tattoo.payment.domain.event.PaymentProcessedEvent;
import com.ericksoares.tattoo.payment.domain.exception.OrderAlreadyPaidException;
import com.ericksoares.tattoo.payment.infrastructure.kafka.PaymentTopics;
import com.ericksoares.tattoo.payment.infrastructure.repository.PaymentRepository;
import com.ericksoares.tattoo.shared.outbox.OutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private ProcessPaymentService service;

    @Test
    void shouldApprovePaymentWhenAmountMatchesOrderTotal() {

        Order order = Order.builder()
                .id(1L)
                .userId(10L)
                .total(BigDecimal.valueOf(100))
                .build();

        PaymentRequest request = new PaymentRequest(BigDecimal.valueOf(100), "PIX");

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.execute(1L, request, 10L, false);

        assertEquals(PaymentStatus.APPROVED, response.status());

        verify(paymentRepository)
                .save(any(Payment.class));

        verify(outboxService).enqueue(
                eq(PaymentTopics.PAYMENT_PROCESSED),
                eq("1"),
                argThat(payload -> payload instanceof PaymentProcessedEvent event
                        && event.status() == PaymentStatus.APPROVED
                        && event.orderId().equals(1L)));
    }

    @Test
    void shouldRejectPaymentWhenAmountDivergesFromOrderTotal() {

        Order order = Order.builder()
                .id(1L)
                .userId(10L)
                .total(BigDecimal.valueOf(100))
                .build();

        PaymentRequest request = new PaymentRequest(BigDecimal.valueOf(50), "PIX");

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.execute(1L, request, 10L, false);

        assertEquals(PaymentStatus.REJECTED, response.status());

        verify(outboxService).enqueue(
                eq(PaymentTopics.PAYMENT_PROCESSED),
                eq("1"),
                argThat(payload -> payload instanceof PaymentProcessedEvent event
                        && event.status() == PaymentStatus.REJECTED
                        && event.orderId().equals(1L)));
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {

        PaymentRequest request = new PaymentRequest(BigDecimal.valueOf(100), "PIX");

        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> service.execute(999L, request, 10L, false)
        );

        verify(paymentRepository, never())
                .save(any());

        verify(outboxService, never())
                .enqueue(any(), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenOrderAlreadyHasApprovedPayment() {

        Order order = Order.builder()
                .id(1L)
                .userId(10L)
                .total(BigDecimal.valueOf(100))
                .build();

        PaymentRequest request = new PaymentRequest(BigDecimal.valueOf(100), "PIX");

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(paymentRepository.existsByOrderIdAndStatus(1L, PaymentStatus.APPROVED))
                .thenReturn(true);

        assertThrows(
                OrderAlreadyPaidException.class,
                () -> service.execute(1L, request, 10L, false)
        );

        verify(paymentRepository, never())
                .save(any());

        verify(outboxService, never())
                .enqueue(any(), any(), any());
    }

    @Test
    void shouldThrowAccessDeniedWhenUserIsNotOwnerNorAdmin() {

        Order order = Order.builder()
                .id(1L)
                .userId(10L)
                .total(BigDecimal.valueOf(100))
                .build();

        PaymentRequest request = new PaymentRequest(BigDecimal.valueOf(100), "PIX");

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        assertThrows(
                AccessDeniedException.class,
                () -> service.execute(1L, request, 999L, false)
        );

        verify(paymentRepository, never())
                .save(any());

        verify(outboxService, never())
                .enqueue(any(), any(), any());
    }

    @Test
    void shouldAllowAdminToPayForAnyOrder() {

        Order order = Order.builder()
                .id(1L)
                .userId(10L)
                .total(BigDecimal.valueOf(100))
                .build();

        PaymentRequest request = new PaymentRequest(BigDecimal.valueOf(100), "PIX");

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.execute(1L, request, 999L, true);

        assertEquals(PaymentStatus.APPROVED, response.status());
    }
}
