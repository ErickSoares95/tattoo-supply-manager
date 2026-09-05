package com.ericksoares.tattoo.shared.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Relays PENDING {@link OutboxEvent} rows to Kafka. Runs on a fixed delay so a broker
 * outage only delays delivery instead of failing the business transaction that produced
 * the event.
 *
 * <p>On a transient publish failure the row stays PENDING (retried next tick) and the
 * batch stops early - if one send failed, the broker is probably unreachable and the
 * rest would fail too. A row that can't be rebuilt (payload class gone) or that keeps
 * failing past {@code outbox.max-attempts} is parked as FAILED for a human to look at.
 */
@Slf4j
@Component
public class OutboxPoller {

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private final int batchSize;
    private final int maxAttempts;
    private final long sendTimeoutMs;

    public OutboxPoller(
            OutboxEventRepository repository,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${outbox.batch-size:100}") int batchSize,
            @Value("${outbox.max-attempts:10}") int maxAttempts,
            @Value("${outbox.send-timeout-ms:10000}") long sendTimeoutMs
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
        this.sendTimeoutMs = sendTimeoutMs;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:5000}")
    @Transactional
    public void publishPending() {

        List<OutboxEvent> pending = repository.findByStatusOrderByIdAsc(
                OutboxStatus.PENDING, PageRequest.of(0, batchSize));

        if (pending.isEmpty()) {
            return;
        }

        log.debug("Outbox poller picked up {} pending event(s)", pending.size());

        for (OutboxEvent event : pending) {
            if (!publish(event)) {
                break;
            }
        }
    }

    /** @return true if the batch should keep going, false if it should stop this tick */
    private boolean publish(OutboxEvent event) {

        Object payload;
        try {
            payload = objectMapper.readValue(event.getPayload(), Class.forName(event.getEventType()));
        } catch (ReflectiveOperationException | RuntimeException | java.io.IOException e) {
            park(event, "Could not rebuild payload: " + e.getMessage());
            log.error("Outbox event {} parked - payload type {} is not usable",
                    event.getId(), event.getEventType(), e);
            return true;
        }

        try {
            kafkaTemplate.send(event.getTopic(), event.getMessageKey(), payload)
                    .get(sendTimeoutMs, TimeUnit.MILLISECONDS);

            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(LocalDateTime.now());
            repository.save(event);

            log.info("Published outbox event {} to topic {}", event.getId(), event.getTopic());
            return true;

        } catch (Exception e) {
            event.setAttempts(event.getAttempts() + 1);
            event.setLastError(e.getMessage());

            if (event.getAttempts() >= maxAttempts) {
                event.setStatus(OutboxStatus.FAILED);
                log.error("Outbox event {} FAILED after {} attempts", event.getId(), event.getAttempts(), e);
            } else {
                log.warn("Outbox event {} publish attempt {} failed, will retry: {}",
                        event.getId(), event.getAttempts(), e.getMessage());
            }
            repository.save(event);
            return event.getStatus() == OutboxStatus.FAILED;
        }
    }

    private void park(OutboxEvent event, String reason) {
        event.setStatus(OutboxStatus.FAILED);
        event.setLastError(reason);
        repository.save(event);
    }
}
