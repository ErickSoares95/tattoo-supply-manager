package com.ericksoares.tattoo.shared.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Entry point for the transactional outbox. Call {@link #enqueue} from inside a
 * {@code @Transactional} business method instead of publishing to Kafka directly:
 * the row is committed atomically with the state change, and {@link OutboxPoller}
 * relays it to the broker afterwards.
 */
@Slf4j
@Service
public class OutboxService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Serializes {@code payload} to JSON and stores it as a PENDING outbox row.
     * Deliberately not annotated with {@code @Transactional}: it must join the
     * caller's transaction so the row and the business state commit or roll back together.
     *
     * @param topic Kafka topic the poller will publish to
     * @param key   Kafka message key (may be null)
     * @param payload the event object; its concrete class is recorded so the poller
     *                can rebuild the exact type and let {@code JsonSerializer} add type headers
     */
    public void enqueue(String topic, String key, Object payload) {

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize outbox payload of type " + payload.getClass(), e);
        }

        OutboxEvent event = OutboxEvent.builder()
                .topic(topic)
                .messageKey(key)
                .eventType(payload.getClass().getName())
                .payload(json)
                .status(OutboxStatus.PENDING)
                .attempts(0)
                .build();

        repository.save(event);

        log.debug("Enqueued outbox event for topic {} (type {})", topic, event.getEventType());
    }
}
