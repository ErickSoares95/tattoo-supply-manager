package com.ericksoares.tattoo.payment.infrastructure.kafka;

import com.ericksoares.tattoo.payment.application.dto.KafkaPingMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Diagnostic-only consumer, round-trips the message produced by {@link KafkaPingProducer}
 * to prove producer + broker + consumer are wired correctly end to end.
 */
@Slf4j
@Component
public class KafkaPingConsumer {

    @KafkaListener(topics = KafkaPingProducer.TOPIC, groupId = "payment-diagnostics")
    public void handle(KafkaPingMessage message) {
        log.info("Kafka ping received on thread {}: {}", Thread.currentThread().getName(), message);
    }
}
