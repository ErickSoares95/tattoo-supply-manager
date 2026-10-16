package com.ericksoares.tattoo.notification.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically retries failed notifications (orders and payments) whose backoff
 * ({@code nextAttemptAt}) has elapsed, on the scheduler thread - never on the notification
 * pool. Scheduling is enabled by shared.config.SchedulingConfig. {@code POST /notifications/reprocess}
 * goes through the same reprocess services.
 */
@Slf4j
@Component
public class NotificationRetryJob {

    private final NotificationReprocessService reprocessService;
    private final PaymentNotificationReprocessService paymentReprocessService;

    public NotificationRetryJob(
            NotificationReprocessService reprocessService,
            PaymentNotificationReprocessService paymentReprocessService
    ) {
        this.reprocessService = reprocessService;
        this.paymentReprocessService = paymentReprocessService;
    }

    @Scheduled(
            fixedDelayString = "${notification.retry.fixed-delay-ms:30000}",
            initialDelayString = "${notification.retry.initial-delay-ms:30000}"
    )
    public void retryDueNotifications() {
        try {
            reprocessService.reprocessDue();
        } catch (Exception e) {
            log.error("Scheduled order notification retry failed", e);
        }

        try {
            paymentReprocessService.reprocessDue();
        } catch (Exception e) {
            log.error("Scheduled payment notification retry failed", e);
        }
    }
}
