package com.ericksoares.tattoo.notification.domain.entity;

import com.ericksoares.tattoo.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

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
}
