package com.ericksoares.tattoo.payment.domain.event;

import com.ericksoares.tattoo.payment.domain.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Published to the {@code payment.processed} Kafka topic (see
 * {@code payment.infrastructure.kafka.PaymentEventProducer}), consumed by the
 * {@code notification} module. Unlike {@code order.domain.event.OrderRegisteredEvent}
 * (in-process, via ApplicationEventPublisher), this event crosses a real broker -
 * eventId exists specifically to make consumers idempotent against redelivery
 * (see notification.event.kafka.PaymentNotificationListener, increment 4).
 */
public record PaymentProcessedEvent(
        UUID eventId,
        Long orderId,
        PaymentStatus status,
        BigDecimal amount,
        LocalDateTime occurredAt
) {}
