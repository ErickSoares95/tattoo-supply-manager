package com.ericksoares.tattoo.notification.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "failed_notifications")
@Data
public class FailedNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private String productName;
    private Integer quantity;

    private String errorMessage;

    private LocalDateTime createdAt;

    private Boolean processed = false;

    // getters/setters
}
