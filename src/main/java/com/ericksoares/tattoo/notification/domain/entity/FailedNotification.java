package com.ericksoares.tattoo.notification.domain.entity;

import com.ericksoares.tattoo.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * One row per (order, sender) that failed to deliver the "order registered" notification.
 *
 * <p>Rows created before {@code senderName} existed have it {@code null}, which means
 * "all senders": they are replayed through every channel (see NotificationReprocessService).
 * {@code productName}/{@code quantity} are legacy columns from when a failure was stored per
 * order item; they are no longer written.
 */
@Entity
@Table(name = "failed_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FailedNotification extends BaseEntity {

    private Long orderId;
    private String productName;
    private Integer quantity;

    private String errorMessage;

    private LocalDateTime createdAt;

    private Boolean processed = false;

    /** Channel that failed ({@code NotificationSender.name()}); null on legacy rows = all senders. */
    private String senderName;

    // Integer + default so ddl-auto=update can add the column to a table that already has rows.
    @Column(columnDefinition = "integer default 0")
    private Integer attempts = 0;

    /** Earliest moment the scheduled job may retry this row; null = due right away. */
    private Instant nextAttemptAt;

    /** Lease held by the worker currently resending this row; null or past = free to claim. */
    private Instant lockedUntil;

    // Optimistic lock; boxed Long + "default 0" so ddl-auto=update can add the column to a
    // table that already has rows (same pattern as Product).
    @Version
    @Column(columnDefinition = "bigint default 0")
    private Long version;

    public int attemptCount() {
        return attempts == null ? 0 : attempts;
    }
}
