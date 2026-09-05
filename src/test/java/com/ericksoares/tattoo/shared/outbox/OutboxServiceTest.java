package com.ericksoares.tattoo.shared.outbox;

import com.ericksoares.tattoo.payment.domain.entity.PaymentStatus;
import com.ericksoares.tattoo.payment.domain.event.PaymentProcessedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private OutboxEventRepository repository;

    @Test
    void shouldStorePendingRowWithSerializedPayloadAndConcreteType() {

        OutboxService service = new OutboxService(repository, objectMapper);

        PaymentProcessedEvent event = new PaymentProcessedEvent(
                UUID.randomUUID(), 42L, PaymentStatus.APPROVED, BigDecimal.valueOf(100), LocalDateTime.now());

        service.enqueue("payment.processed", "42", event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getTopic()).isEqualTo("payment.processed");
        assertThat(saved.getMessageKey()).isEqualTo("42");
        assertThat(saved.getEventType()).isEqualTo(PaymentProcessedEvent.class.getName());
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getAttempts()).isZero();
        assertThat(saved.getPayload()).contains("\"orderId\":42").contains("APPROVED");
    }

    @Test
    void shouldThrowWhenPayloadCannotBeSerialized() {

        OutboxService service = new OutboxService(repository, objectMapper);

        // bare Object -> "no properties discovered", Jackson fails on empty beans
        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> service.enqueue("t", "k", new Object())))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
