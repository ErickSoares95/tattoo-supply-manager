package com.ericksoares.tattoo.payment.infrastructure.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the payment module's Kafka topics. The local docker-compose broker has
 * auto-create on, but managed brokers (e.g. Redpanda Serverless) don't - KafkaAdmin
 * creates any missing ones on startup, using broker-default partitions/replication
 * (so this stays valid whether the broker forces replication factor 1 or 3).
 */
@Configuration
public class PaymentKafkaConfig {

    @Bean
    public NewTopic paymentProcessedTopic() {
        return TopicBuilder.name(PaymentTopics.PAYMENT_PROCESSED).build();
    }

    @Bean
    public NewTopic paymentProcessedDltTopic() {
        return TopicBuilder.name(PaymentTopics.PAYMENT_PROCESSED + ".DLT").build();
    }

    @Bean
    public NewTopic kafkaPingTopic() {
        return TopicBuilder.name(KafkaPingProducer.TOPIC).build();
    }
}
