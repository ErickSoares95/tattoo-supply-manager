package com.ericksoares.tattoo.payment.infrastructure.kafka;

import com.ericksoares.tattoo.payment.domain.event.PaymentProcessedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentEventProducer {

    public static final String TOPIC = "payment.processed";

    private final KafkaTemplate<String, PaymentProcessedEvent> kafkaTemplate;

    public PaymentEventProducer(KafkaTemplate<String, PaymentProcessedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Key = orderId, so every event for the same order lands in the same partition
     * and is consumed in publish order (relevant if an order is ever paid more than once).
     */
    public void publish(PaymentProcessedEvent event) {

        kafkaTemplate.send(TOPIC, event.orderId().toString(), event);

        log.info("Published PaymentProcessedEvent {} for order {} (status {})",
                event.eventId(), event.orderId(), event.status());
    }
}
