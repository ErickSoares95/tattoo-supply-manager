package com.ericksoares.tattoo.notification.event.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.ericksoares.tattoo.notification.application.dto.SenderFailure;
import com.ericksoares.tattoo.notification.application.service.FailedPaymentNotificationRecorder;
import com.ericksoares.tattoo.notification.application.service.NotificationService;
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
import java.util.List;

/**
 * Kafka counterpart of {@code notification.event.NotificationListener}: same
 * NotificationService/FailedNotification-style resilience, but reacting to a real
 * broker topic (payment.processed) instead of an in-process ApplicationEventPublisher event.
 *
 * Idempotent against Kafka's at-least-once redelivery (retry, rebalance) via a
 * "claim before processing" pattern - see {@link #claim}.
 */
@Slf4j
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
@Component
public class PaymentNotificationListener {

    private final NotificationService notificationService;
    private final FailedPaymentNotificationRecorder recorder;
    private final ProcessedPaymentEventRepository processedRepository;
    private final FailedPaymentNotificationRepository failedRepository;

    public PaymentNotificationListener(
            NotificationService notificationService,
            FailedPaymentNotificationRecorder recorder,
            ProcessedPaymentEventRepository processedRepository,
            FailedPaymentNotificationRepository failedRepository
    ) {
        this.notificationService = notificationService;
        this.recorder = recorder;
        this.processedRepository = processedRepository;
        this.failedRepository = failedRepository;
    }

    @KafkaListener(topics = PaymentTopics.PAYMENT_PROCESSED, groupId = "notification-service")
    public void handle(PaymentProcessedEvent event) {

        if (!claim(event)) {
            log.info("PaymentProcessedEvent {} already processed, skipping (redelivery)", event.eventId());
            return;
        }

        log.info("Received PaymentProcessedEvent for order {} (status {})", event.orderId(), event.status());
        log.info("Processing in thread: {}", Thread.currentThread().getName());

        List<SenderFailure> failures = switch (event.status()) {
            case APPROVED -> notificationService.notifyPaymentConfirmed(event.orderId());
            case REJECTED -> notificationService.notifyPaymentRejected(event.orderId());
            case PENDING -> {
                log.warn("Received PaymentProcessedEvent with unexpected status PENDING for order {}", event.orderId());
                yield List.of();
            }
        };

        persistFailures(event, failures);
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

    /**
     * One row per failed sender, written by the recorder in its own transaction; the scheduled
     * job (PaymentNotificationReprocessService) retries each one only through its sender.
     */
    private void persistFailures(PaymentProcessedEvent event, List<SenderFailure> failures) {

        for (SenderFailure failure : failures) {

            // A previous delivery of this event already recorded this sender: do not duplicate the row.
            if (failedRepository.existsByEventIdAndSenderName(event.eventId(), failure.senderName())) {
                log.info("Failed payment notification for event {} (sender {}) already recorded, skipping",
                        event.eventId(), failure.senderName());
                continue;
            }

            try {
                recorder.recordFailure(
                        event.eventId(), event.orderId(), event.status().name(), event.amount(), failure);

                log.warn("Failed payment notification persisted for order {} (sender {})",
                        event.orderId(), failure.senderName());
            } catch (RuntimeException e) {
                // The failure could not be saved, so nothing would ever retry this sender: give the claim
                // back and let the Kafka error handler redeliver (or send to the DLT).
                releaseClaim(event, e);
                throw e;
            }
        }
    }

    /**
     * Compensation for a lost failure record. If it fails too, the original exception still propagates
     * (the compensation error is attached as suppressed) and the event is logged for manual recovery.
     */
    private void releaseClaim(PaymentProcessedEvent event, RuntimeException cause) {
        try {
            processedRepository.releaseClaim(event.eventId());

            log.warn("Could not persist failed payment notification for order {} - claim of event {} released for redelivery",
                    event.orderId(), event.eventId(), cause);
        } catch (RuntimeException compensationError) {
            cause.addSuppressed(compensationError);

            log.error("Could not persist failed payment notification NOR release claim of event {} (order {}) - manual recovery needed",
                    event.eventId(), event.orderId(), compensationError);
        }
    }
}
