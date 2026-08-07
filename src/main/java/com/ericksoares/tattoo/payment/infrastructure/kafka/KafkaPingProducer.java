package com.ericksoares.tattoo.payment.infrastructure.kafka;

import com.ericksoares.tattoo.payment.application.dto.KafkaPingMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Diagnostic-only producer, used to validate the Kafka wiring (broker reachable,
 * serializer config correct) before any real payment domain code depends on it.
 */
@Slf4j
@Component
public class KafkaPingProducer {

    public static final String TOPIC = "diagnostics.kafka-ping";

    private final KafkaTemplate<String, KafkaPingMessage> kafkaTemplate;

    public KafkaPingProducer(KafkaTemplate<String, KafkaPingMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(String text) {

        KafkaPingMessage message = new KafkaPingMessage(text, LocalDateTime.now());

        kafkaTemplate.send(TOPIC, message);

        log.info("Kafka ping sent to topic {}: {}", TOPIC, message);
    }
}
