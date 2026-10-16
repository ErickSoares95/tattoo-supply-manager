package com.ericksoares.tattoo.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Published in-process by {@code ProcessPaymentService} (ApplicationEventPublisher), like
 * {@code order.OrderRegisteredEvent}, and consumed AFTER_COMMIT by
 * {@code notification.PaymentNotificationListener}. eventId is a correlation id: it is
 * stored on every {@code failed_payment_notifications} row the event produces.
 */
public record PaymentProcessedEvent(
        UUID eventId,
        Long orderId,
        PaymentStatus status,
        BigDecimal amount,
        LocalDateTime occurredAt
) {}
