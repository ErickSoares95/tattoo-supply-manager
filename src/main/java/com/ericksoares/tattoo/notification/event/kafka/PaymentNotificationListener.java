package com.ericksoares.tattoo.notification.event.kafka;

import com.ericksoares.tattoo.notification.application.service.NotificationService;
import com.ericksoares.tattoo.notification.domain.entity.FailedPaymentNotification;
import com.ericksoares.tattoo.notification.domain.entity.ProcessedPaymentEvent;
import com.ericksoares.tattoo.notification.domain.repository.FailedPaymentNotificationRepository;
import com.ericksoares.tattoo.notification.domain.repository.ProcessedPaymentEventRepository;
import com.ericksoares.tattoo.payment.domain.event.PaymentProcessedEvent;
import com.ericksoares.tattoo.payment.infrastructure.kafka.PaymentTopics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Kafka counterpart of {@code notification.event.NotificationListener}: same
 * NotificationService/FailedNotification-style resilience, but reacting to a real
 * broker topic (payment.processed) instead of an in-process ApplicationEventPublisher event.
 *
 * Idempotent against Kafka's at-least-once redelivery (retry, rebalance) via a
 * "claim before processing" pattern - see {@link #claim}.
 */
@Slf4j
@Component
public class PaymentNotificationListener {

    private final NotificationService notificationService;
    private final FailedPaymentNotificationRepository failedRepository;
    private final ProcessedPaymentEventRepository processedRepository;

    public PaymentNotificationListener(
            NotificationService notificationService,
            FailedPaymentNotificationRepository failedRepository,
            ProcessedPaymentEventRepository processedRepository
    ) {
        this.notificationService = notificationService;
        this.failedRepository = failedRepository;
        this.processedRepository = processedRepository;
    }

    @KafkaListener(topics = PaymentTopics.PAYMENT_PROCESSED, groupId = "notification-service")
    public void handle(PaymentProcessedEvent event) {

        if (!claim(event)) {
            log.info("PaymentProcessedEvent {} already processed, skipping (redelivery)", event.eventId());
            return;
        }

        log.info("Received PaymentProcessedEvent for order {} (status {})", event.orderId(), event.status());
        log.info("Processing in thread: {}", Thread.currentThread().getName());

        boolean succeeded = switch (event.status()) {
            case APPROVED -> notificationService.notifyPaymentConfirmed(event.orderId());
            case REJECTED -> notificationService.notifyPaymentRejected(event.orderId());
            case PENDING -> {
                log.warn("Received PaymentProcessedEvent with unexpected status PENDING for order {}", event.orderId());
                yield true;
            }
        };

        if (!succeeded) {
            persistFailure(event);
        }
    }

    /**
     * Tries to insert the event's id first. The unique constraint on event_id lets the
     * database arbitrate: only the first attempt (across threads/instances) succeeds,
     * so a concurrent redelivery can never slip through a check-then-act race window.
     */
    private boolean claim(PaymentProcessedEvent event) {
        try {
            processedRepository.saveAndFlush(
                    ProcessedPaymentEvent.builder()
                            .eventId(event.eventId())
                            .processedAt(LocalDateTime.now())
                            .build()
            );
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    private void persistFailure(PaymentProcessedEvent event) {

        FailedPaymentNotification failure = new FailedPaymentNotification();

        failure.setOrderId(event.orderId());
        failure.setStatus(event.status().name());
        failure.setAmount(event.amount());
        failure.setErrorMessage("Failed to deliver payment notification to one or more senders");
        failure.setCreatedAt(LocalDateTime.now());
        failure.setProcessed(false);

        failedRepository.save(failure);

        log.warn("Failed payment notification persisted for order {}", event.orderId());
    }
}
