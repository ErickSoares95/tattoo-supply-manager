package com.ericksoares.tattoo.payment.domain.entity;

import org.springframework.modulith.NamedInterface;

/** Carried by {@code PaymentProcessedEvent}; part of the payment module's public API. */
@NamedInterface("api")
public enum PaymentStatus {
    PENDING,
    APPROVED,
    REJECTED
}
