package com.ericksoares.tattoo.shared.outbox;

import com.ericksoares.tattoo.payment.domain.entity.PaymentStatus;
import com.ericksoares.tattoo.payment.domain.event.PaymentProcessedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private OutboxPoller poller;

    @BeforeEach
    void setUp() {
        poller = new OutboxPoller(repository, kafkaTemplate, objectMapper, 100, 3, 1000);
    }

    private OutboxEvent pendingRow(long id) {
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                UUID.randomUUID(), id, PaymentStatus.APPROVED, BigDecimal.TEN, LocalDateTime.now());
        try {
            OutboxEvent row = OutboxEvent.builder()
                    .topic("payment.processed")
                    .messageKey(String.valueOf(id))
                    .eventType(PaymentProcessedEvent.class.getName())
                    .payload(objectMapper.writeValueAsString(event))
                    .status(OutboxStatus.PENDING)
                    .attempts(0)
                    .build();
            row.setId(id);
            return row;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldPublishPendingRowAndMarkItPublished() {

        OutboxEvent row = pendingRow(1L);
        when(repository.findByStatusOrderByIdAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(row));
        when(kafkaTemplate.send(eq("payment.processed"), eq("1"), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        poller.publishPending();

        verify(kafkaTemplate).send(eq("payment.processed"), eq("1"), any(PaymentProcessedEvent.class));
        assertThat(row.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(row.getPublishedAt()).isNotNull();
        verify(repository).save(row);
    }

    @Test
    void shouldKeepRowPendingAndStopBatchWhenPublishFails() {

        OutboxEvent first = pendingRow(1L);
        OutboxEvent second = pendingRow(2L);
        when(repository.findByStatusOrderByIdAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(first, second));
        when(kafkaTemplate.send(anyString(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        poller.publishPending();

        // first row failed -> batch stops, second row is never attempted
        verify(kafkaTemplate, times(1)).send(anyString(), any(), any());
        assertThat(first.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(first.getAttempts()).isEqualTo(1);
        assertThat(first.getLastError()).contains("broker down");
        assertThat(second.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(second.getAttempts()).isZero();
    }

    @Test
    void shouldParkRowAsFailedAfterMaxAttempts() {

        OutboxEvent row = pendingRow(1L);
        row.setAttempts(2); // maxAttempts = 3, so this attempt is the last
        when(repository.findByStatusOrderByIdAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(row));
        when(kafkaTemplate.send(anyString(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("still down")));

        poller.publishPending();

        assertThat(row.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(row.getAttempts()).isEqualTo(3);
    }

    @Test
    void shouldParkRowWhenPayloadTypeCannotBeResolved() {

        OutboxEvent row = pendingRow(1L);
        row.setEventType("com.ericksoares.tattoo.does.not.Exist");
        when(repository.findByStatusOrderByIdAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(row));

        poller.publishPending();

        assertThat(row.getStatus()).isEqualTo(OutboxStatus.FAILED);
        verify(kafkaTemplate, never()).send(anyString(), any(), any());
    }

    @Test
    void shouldDoNothingWhenNoPendingRows() {

        when(repository.findByStatusOrderByIdAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of());

        poller.publishPending();

        verifyNoInteractions(kafkaTemplate);
    }
}
