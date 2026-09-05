package com.ericksoares.tattoo.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables {@code @Scheduled} support. Currently drives {@code shared.outbox.OutboxPoller},
 * which relays transactional-outbox rows to Kafka.
 */
@EnableScheduling
@Configuration
public class SchedulingConfig {
}
