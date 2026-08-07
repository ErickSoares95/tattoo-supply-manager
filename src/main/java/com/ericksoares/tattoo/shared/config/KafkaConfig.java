package com.ericksoares.tattoo.shared.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Consumer-side resilience for @KafkaListener beans (picked up automatically by
 * Spring Boot's auto-configured ConcurrentKafkaListenerContainerFactory - a single
 * CommonErrorHandler bean in the context is enough, no extra wiring needed).
 *
 * On a processing failure: retries a few times with a fixed delay: if it still fails,
 * the message is republished to "<original-topic>.DLT" and the offset advances - the
 * consumer never gets stuck forever on one bad message, and the message isn't silently lost.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition())
        );

        FixedBackOff backOff = new FixedBackOff(1000L, 3L);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
