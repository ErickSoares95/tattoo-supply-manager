package com.ericksoares.tattoo.shared.outbox;

import com.ericksoares.tattoo.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Transactional outbox row. Written in the same transaction as the state change that
 * produced it (see {@link OutboxService#enqueue}), then relayed to Kafka by
 * {@link OutboxPoller} on a separate tick. This is what makes "save to DB" and
 * "publish to broker" effectively atomic without a distributed transaction: if the
 * app crashes right after commit, the row is still here to be published on restart.
 *
 * <p>Consumers must stay idempotent - the poller guarantees at-least-once, not
 * exactly-once (a crash between {@code send} and {@code status = PUBLISHED} re-sends).
 * The {@code payment.processed} consumer already dedups via {@code ProcessedPaymentEvent}.
 */
@Entity
@Table(name = "outbox_events", indexes = @Index(name = "idx_outbox_status", columnList = "status"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OutboxEvent extends BaseEntity {

    @Column(nullable = false)
    private String topic;

    /** Kafka message key (nullable); drives partitioning / per-key ordering. */
    private String messageKey;

    /** Fully-qualified class name of the payload, so the poller can rebuild the exact type. */
    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    private LocalDateTime publishedAt;

    private String lastError;
}
