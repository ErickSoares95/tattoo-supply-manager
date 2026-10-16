package com.ericksoares.tattoo.payment;

import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;

/**
 * Test-only entry point for other modules' integration tests: Payment and PaymentRepository
 * are package-private, so tests outside the payment module persist payments here.
 */
public final class PaymentTestFixtures {

    private PaymentTestFixtures() {}

    /** Persists a payment directly (no order checks, no event) and returns its id. */
    public static Long savePayment(ApplicationContext context, Long orderId, BigDecimal amount,
                                   String method, PaymentStatus status) {
        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .method(method)
                .status(status)
                .build();
        return context.getBean(PaymentRepository.class).save(payment).getId();
    }
}
