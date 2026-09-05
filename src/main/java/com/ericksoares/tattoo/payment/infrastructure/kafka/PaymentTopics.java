package com.ericksoares.tattoo.payment.infrastructure.kafka;

/**
 * Kafka topic names owned by the payment module. The DLT counterpart is created by
 * {@code shared.config.KafkaConfig} and populated by the consumer-side error handler.
 */
public final class PaymentTopics {

    private PaymentTopics() {
    }

    public static final String PAYMENT_PROCESSED = "payment.processed";
}
