package com.ericksoares.tattoo.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

record PaymentResponse(
        Long id,
        Long orderId,
        BigDecimal amount,
        String method,
        PaymentStatus status,
        LocalDateTime creationDate
) {}
