package com.ericksoares.tattoo.notification.application.service;

import com.ericksoares.tattoo.notification.application.dto.SenderFailure;
import com.ericksoares.tattoo.notification.domain.entity.FailedPaymentNotification;
import com.ericksoares.tattoo.notification.domain.exception.PermanentNotificationException;
import com.ericksoares.tattoo.notification.domain.repository.FailedPaymentNotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.LongFunction;

/**
 * Replays failed payment notifications from the {@code failed_payment_notifications} table,
 * only through the sender that failed, guarded by an atomic lease like
 * {@link NotificationReprocessService}.
 */
@Service
@Slf4j
public class PaymentNotificationReprocessService {

    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";

    private final FailedPaymentNotificationRepository repository;
    private final NotificationService notificationService;
    private final FailedPaymentNotificationRecorder recorder;

    public PaymentNotificationReprocessService(
            FailedPaymentNotificationRepository repository,
            NotificationService notificationService,
            FailedPaymentNotificationRecorder recorder
    ) {
        this.repository = repository;
        this.notificationService = notificationService;
        this.recorder = recorder;
    }

    /** Manual replay: every unprocessed failure with attempts left, regardless of its backoff. */
    public void reprocessAll() {
        Instant now = Instant.now();

        reprocessBatches(lastId -> repository.findRetryable(
                NotificationRetryPolicy.MAX_ATTEMPTS,
                now,
                lastId,
                PageRequest.ofSize(NotificationReprocessService.BATCH_SIZE)
        ));
    }

    /** Scheduled replay: only failures whose backoff has elapsed and that still have attempts left. */
    public void reprocessDue() {
        Instant now = Instant.now();

        reprocessBatches(lastId -> repository.findDue(
                NotificationRetryPolicy.MAX_ATTEMPTS,
                now,
                lastId,
                PageRequest.ofSize(NotificationReprocessService.BATCH_SIZE)
        ));
    }

    private void reprocessBatches(LongFunction<List<FailedPaymentNotification>> nextBatch) {

        long lastId = 0L;

        while (true) {

            List<FailedPaymentNotification> batch = nextBatch.apply(lastId);

            if (batch.isEmpty()) {
                return;
            }

            for (FailedPaymentNotification failure : batch) {
                reprocess(failure);
                lastId = failure.getId();
            }
        }
    }

    private void reprocess(FailedPaymentNotification failure) {

        try {

            Instant now = Instant.now();

            if (repository.claim(failure.getId(), now.plus(NotificationReprocessService.LEASE), now) != 1) {
                log.info("Failed payment notification {} is processed or being handled elsewhere, skipping",
                        failure.getId());
                return;
            }

            if (!APPROVED.equals(failure.getStatus()) && !REJECTED.equals(failure.getStatus())) {
                recorder.recordRetryFailure(failure.getId(), new PermanentNotificationException(
                        "Unsupported payment status " + failure.getStatus()));
                return;
            }

            if (failure.getSenderName() == null) {
                reprocessLegacy(failure);
                return;
            }

            Optional<SenderFailure> result = APPROVED.equals(failure.getStatus())
                    ? notificationService.notifyPaymentConfirmed(failure.getOrderId(), failure.getSenderName())
                    : notificationService.notifyPaymentRejected(failure.getOrderId(), failure.getSenderName());

            if (result.isEmpty()) {
                repository.markProcessed(failure.getId());
            } else {
                log.warn("Payment reprocess failed again for id {} (sender {})",
                        failure.getId(), failure.getSenderName());
                recorder.recordRetryFailure(failure.getId(), result.get().error());
            }

        } catch (Exception e) {
            log.error("Unexpected error reprocessing payment notification id {}", failure.getId(), e);
        }
    }

    /**
     * Rows written before sender tracking existed (senderName == null) mean "all senders": replay
     * once through every channel and replace with per-sender rows for the ones that still fail.
     */
    private void reprocessLegacy(FailedPaymentNotification failure) {

        List<SenderFailure> failures = APPROVED.equals(failure.getStatus())
                ? notificationService.notifyPaymentConfirmed(failure.getOrderId())
                : notificationService.notifyPaymentRejected(failure.getOrderId());

        for (SenderFailure senderFailure : failures) {
            recorder.recordFailure(
                    failure.getEventId(), failure.getOrderId(), failure.getStatus(), failure.getAmount(), senderFailure);
        }

        repository.markProcessed(failure.getId());
    }
}
