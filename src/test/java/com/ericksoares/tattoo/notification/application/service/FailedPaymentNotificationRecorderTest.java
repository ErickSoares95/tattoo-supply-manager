package com.ericksoares.tattoo.notification.application.service;

import com.ericksoares.tattoo.notification.application.dto.SenderFailure;
import com.ericksoares.tattoo.notification.domain.entity.FailedPaymentNotification;
import com.ericksoares.tattoo.notification.domain.exception.PermanentNotificationException;
import com.ericksoares.tattoo.notification.domain.repository.FailedPaymentNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FailedPaymentNotificationRecorderTest {

    @Mock
    private FailedPaymentNotificationRepository repository;

    private FailedPaymentNotificationRecorder recorder() {
        return new FailedPaymentNotificationRecorder(repository);
    }

    private static void assertAround(Instant expected, Instant actual) {
        assertTrue(Duration.between(expected, actual).abs().compareTo(Duration.ofSeconds(5)) < 0,
                "expected ~" + expected + " but was " + actual);
    }

    @Test
    void shouldRecordFirstFailureAsAttemptOneWithFirstBackoff() {

        UUID eventId = UUID.randomUUID();

        recorder().recordFailure(eventId, 7L, "APPROVED", BigDecimal.TEN,
                new SenderFailure("WEBHOOK", new RuntimeException("down")));

        ArgumentCaptor<FailedPaymentNotification> saved = ArgumentCaptor.forClass(FailedPaymentNotification.class);
        verify(repository).save(saved.capture());

        FailedPaymentNotification row = saved.getValue();
        assertEquals(eventId, row.getEventId());
        assertEquals(7L, row.getOrderId());
        assertEquals("APPROVED", row.getStatus());
        assertEquals("WEBHOOK", row.getSenderName());
        assertEquals(1, row.getAttempts());
        assertFalse(row.getProcessed());
        assertAround(Instant.now().plusSeconds(30), row.getNextAttemptAt());
    }

    @Test
    void shouldRecordPermanentFirstFailureAsTerminalRightAway() {

        recorder().recordFailure(UUID.randomUUID(), 7L, "REJECTED", BigDecimal.TEN,
                new SenderFailure("WEBHOOK", new PermanentNotificationException("bad")));

        ArgumentCaptor<FailedPaymentNotification> saved = ArgumentCaptor.forClass(FailedPaymentNotification.class);
        verify(repository).save(saved.capture());

        assertEquals(NotificationRetryPolicy.MAX_ATTEMPTS, saved.getValue().getAttempts());
        assertNull(saved.getValue().getNextAttemptAt());
    }

    @Test
    void shouldDoubleTheBackoffAndReleaseTheLeaseOnEachFailedRetry() {

        FailedPaymentNotification row = new FailedPaymentNotification();
        row.setAttempts(2);
        row.setLockedUntil(Instant.now().plusSeconds(120));
        when(repository.findById(5L)).thenReturn(Optional.of(row));

        recorder().recordRetryFailure(5L, new RuntimeException("still down"));

        assertEquals(3, row.getAttempts());
        assertNull(row.getLockedUntil());
        assertAround(Instant.now().plusSeconds(120), row.getNextAttemptAt());
        verify(repository).save(row);
    }

    @Test
    void shouldParkPermanentRetryFailureAsTerminal() {

        FailedPaymentNotification row = new FailedPaymentNotification();
        row.setAttempts(1);
        row.setNextAttemptAt(Instant.now());
        when(repository.findById(5L)).thenReturn(Optional.of(row));

        recorder().recordRetryFailure(5L, new PermanentNotificationException("invalid"));

        assertEquals(NotificationRetryPolicy.MAX_ATTEMPTS, row.getAttempts());
        assertNull(row.getNextAttemptAt());
    }

    @Test
    void shouldNotOverwriteARowThatWasProcessedMeanwhile() {

        FailedPaymentNotification row = new FailedPaymentNotification();
        row.setProcessed(true);
        when(repository.findById(5L)).thenReturn(Optional.of(row));

        recorder().recordRetryFailure(5L, new RuntimeException("down"));

        verify(repository, never()).save(any());
    }
}
