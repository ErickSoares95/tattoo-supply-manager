package com.ericksoares.tattoo.payment.application.dto;

import com.ericksoares.tattoo.payment.domain.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long orderId,
        BigDecimal amount,
        String method,
        PaymentStatus status,
        LocalDateTime creationDate
) {}
