package com.ericksoares.tattoo.notification.domain.entity;

import com.ericksoares.tattoo.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Idempotency marker for {@code payment.processed} Kafka messages already handled by
 * this consumer group. See notification.event.kafka.PaymentNotificationListener:
 * a row here is "claimed" before processing (INSERT-first, unique constraint on
 * event_id) so at-least-once redelivery never sends a duplicate notification.
 */
@Entity
@Table(name = "processed_payment_events")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class ProcessedPaymentEvent extends BaseEntity {

    @Column(nullable = false, unique = true)
    private UUID eventId;

    private LocalDateTime processedAt;
}
