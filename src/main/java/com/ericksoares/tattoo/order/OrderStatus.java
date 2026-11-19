package com.ericksoares.tattoo.order;

/** PENDING_PAYMENT -> PAID (approved payment) or CANCELLED (owner/admin, or expired unpaid). */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    CANCELLED
}
