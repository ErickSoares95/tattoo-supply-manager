package com.ericksoares.tattoo.notification.domain.entity;

import com.ericksoares.tattoo.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "failed_payment_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FailedPaymentNotification extends BaseEntity {

    private Long orderId;
    private String status;
    private BigDecimal amount;

    private String errorMessage;

    private LocalDateTime createdAt;

    private Boolean processed = false;
}
