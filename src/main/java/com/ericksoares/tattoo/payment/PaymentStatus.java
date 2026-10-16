package com.ericksoares.tattoo.payment;

/** Carried by {@code PaymentProcessedEvent}; part of the payment module's public API. */
public enum PaymentStatus {
    PENDING,
    APPROVED,
    REJECTED
}
