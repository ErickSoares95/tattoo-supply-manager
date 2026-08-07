package com.ericksoares.tattoo.notification.event.kafka;

import com.ericksoares.tattoo.notification.application.service.NotificationService;
import com.ericksoares.tattoo.notification.domain.entity.FailedPaymentNotification;
import com.ericksoares.tattoo.notification.domain.repository.FailedPaymentNotificationRepository;
import com.ericksoares.tattoo.payment.domain.event.PaymentProcessedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Kafka counterpart of {@code notification.event.NotificationListener}: same
 * NotificationService/FailedNotification-style resilience, but reacting to a real
 * broker topic (payment.processed) instead of an in-process ApplicationEventPublisher event.
 */
@Slf4j
@Component
public class PaymentNotificationListener {

    private final NotificationService notificationService;
    private final FailedPaymentNotificationRepository failedRepository;

    public PaymentNotificationListener(
            NotificationService notificationService,
            FailedPaymentNotificationRepository failedRepository
    ) {
        this.notificationService = notificationService;
        this.failedRepository = failedRepository;
    }

    @KafkaListener(topics = "payment.processed", groupId = "notification-service")
    public void handle(PaymentProcessedEvent event) {

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
