package com.ericksoares.tattoo.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables {@code @Scheduled} support. Currently drives
 * {@code notification.NotificationRetryJob}, which replays failed order
 * and payment notifications whose backoff has elapsed.
 */
@EnableScheduling
@Configuration
class SchedulingConfig {
}
