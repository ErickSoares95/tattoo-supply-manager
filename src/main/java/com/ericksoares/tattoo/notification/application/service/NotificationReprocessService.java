package com.ericksoares.tattoo.notification.application.service;

import com.ericksoares.tattoo.notification.application.dto.NotificationContext;
import com.ericksoares.tattoo.notification.application.dto.SenderFailure;
import com.ericksoares.tattoo.notification.domain.entity.FailedNotification;
import com.ericksoares.tattoo.notification.domain.repository.FailedNotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.LongFunction;

@Service
@Slf4j
public class NotificationReprocessService {

    static final int BATCH_SIZE = 50;

    /** How long a worker owns a row while resending it; no DB transaction is held meanwhile. */
    static final Duration LEASE = Duration.ofMinutes(2);

    private final FailedNotificationRepository repository;
    private final NotificationService notificationService;
    private final FailedNotificationRecorder recorder;

    public NotificationReprocessService(
            FailedNotificationRepository repository,
            NotificationService notificationService,
            FailedNotificationRecorder recorder
    ) {
        this.repository = repository;
        this.notificationService = notificationService;
        this.recorder = recorder;
    }

    /**
     * Manual replay ({@code POST /notifications/reprocess}): every unprocessed failure that still
     * has attempts left, regardless of its backoff.
     */
    public void reprocessAll() {
        Instant now = Instant.now();

        reprocessBatches(lastId -> repository.findRetryable(
                NotificationRetryPolicy.MAX_ATTEMPTS,
                now,
                lastId,
                PageRequest.ofSize(BATCH_SIZE)
        ));
    }

    /**
     * Scheduled replay: only failures whose backoff has elapsed and that still have attempts left.
     */
    public void reprocessDue() {
        Instant now = Instant.now();

        reprocessBatches(lastId -> repository.findDue(
                NotificationRetryPolicy.MAX_ATTEMPTS,
                now,
                lastId,
                PageRequest.ofSize(BATCH_SIZE)
        ));
    }

    /**
     * Walks the failures in id-ordered batches (keyset pagination, so a row that fails again
     * cannot be picked up twice in one run).
     */
    private void reprocessBatches(LongFunction<List<FailedNotification>> nextBatch) {

        long lastId = 0L;

        while (true) {

            List<FailedNotification> batch = nextBatch.apply(lastId);

            if (batch.isEmpty()) {
                return;
            }

            for (FailedNotification failure : batch) {
                reprocess(failure);
                lastId = failure.getId();
            }
        }
    }

    private void reprocess(FailedNotification failure) {

        try {

            // Lease the row first: the conditional UPDATE lets exactly one worker (job, POST or
            // another instance) send, so a row is never delivered twice at the same time.
            Instant now = Instant.now();

            if (repository.claim(failure.getId(), now.plus(LEASE), now) != 1) {
                log.info("Failed notification {} is processed or being handled elsewhere, skipping", failure.getId());
                return;
            }

            NotificationContext context = new NotificationContext(failure.getOrderId());

            if (failure.getSenderName() == null) {
                reprocessLegacy(failure, context);
                return;
            }

            Optional<SenderFailure> result =
                    notificationService.notifyOrderRegistered(context, failure.getSenderName());

            if (result.isEmpty()) {
                repository.markProcessed(failure.getId());
            } else {
                log.warn("Reprocess failed again for id {} (sender {})", failure.getId(), failure.getSenderName());
                recorder.recordRetryFailure(failure.getId(), result.get().error());
            }

        } catch (Exception e) {
            log.error("Unexpected error reprocessing id {}", failure.getId(), e);
        }
    }

    /**
     * Rows written before sender tracking existed (senderName == null) mean "all senders".
     * They are replayed through every channel once and replaced by per-sender rows for
     * whichever channels still fail, so from then on retries are targeted. The original row
     * is claimed by the caller first, so a concurrent worker cannot create duplicate rows.
     */
    private void reprocessLegacy(FailedNotification failure, NotificationContext context) {

        List<SenderFailure> failures = notificationService.notifyOrderRegistered(context);

        for (SenderFailure senderFailure : failures) {
            recorder.recordFailure(failure.getOrderId(), senderFailure);
        }

        repository.markProcessed(failure.getId());
    }
}
