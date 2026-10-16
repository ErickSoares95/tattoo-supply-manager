package com.ericksoares.tattoo.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FailedNotificationRecorderTest {

    @Mock
    private FailedNotificationRepository repository;

    private FailedNotificationRecorder recorder() {
        return new FailedNotificationRecorder(repository);
    }

    private static void assertAround(Instant expected, Instant actual) {
        assertTrue(Duration.between(expected, actual).abs().compareTo(Duration.ofSeconds(5)) < 0,
                "expected ~" + expected + " but was " + actual);
    }

    @Test
    void shouldRecordFirstFailureAsAttemptOneWithFirstBackoff() {

        recorder().recordFailure(7L, new SenderFailure("WEBHOOK", new RuntimeException("down")));

        ArgumentCaptor<FailedNotification> saved = ArgumentCaptor.forClass(FailedNotification.class);
        verify(repository).save(saved.capture());

        FailedNotification row = saved.getValue();
        assertEquals(7L, row.getOrderId());
        assertEquals("WEBHOOK", row.getSenderName());
        assertEquals(1, row.getAttempts());
        assertFalse(row.getProcessed());
        assertAround(Instant.now().plusSeconds(30), row.getNextAttemptAt());
    }

    @Test
    void shouldDoubleTheBackoffOnEachFailedRetry() {

        FailedNotification row = new FailedNotification();
        row.setOrderId(7L);
        row.setSenderName("WEBHOOK");
        row.setAttempts(2);

        when(repository.findById(5L)).thenReturn(Optional.of(row));
        recorder().recordRetryFailure(5L, new RuntimeException("still down"));

        assertEquals(3, row.getAttempts());
        assertAround(Instant.now().plusSeconds(120), row.getNextAttemptAt());
        verify(repository).save(row);
    }

    @Test
    void shouldTreatNullAttemptsOfLegacyRowsAsZero() {

        FailedNotification row = new FailedNotification();
        row.setAttempts(null);

        when(repository.findById(5L)).thenReturn(Optional.of(row));
        recorder().recordRetryFailure(5L, new RuntimeException("down"));

        assertEquals(1, row.getAttempts());
    }

    @Test
    void shouldRecordRejectedTaskWithZeroAttemptsDueNow() {

        when(repository.save(any(FailedNotification.class))).thenAnswer(i -> i.getArgument(0));

        FailedNotification row = recorder().recordRejected(7L, "EMAIL");

        assertEquals(0, row.getAttempts());
        assertEquals("EMAIL", row.getSenderName());
        assertFalse(row.getProcessed());
        assertAround(Instant.now(), row.getNextAttemptAt());
    }

    @Test
    void shouldParkPermanentFailureAsTerminalWithoutNextAttempt() {

        FailedNotification row = new FailedNotification();
        row.setOrderId(7L);
        row.setSenderName("WEBHOOK");
        row.setAttempts(1);
        row.setNextAttemptAt(Instant.now());

        when(repository.findById(5L)).thenReturn(Optional.of(row));
        recorder().recordRetryFailure(5L, new PermanentNotificationException("invalid payload"));

        assertEquals(NotificationRetryPolicy.MAX_ATTEMPTS, row.getAttempts());
        assertTrue(NotificationRetryPolicy.isExhausted(row.getAttempts()));
        assertNull(row.getNextAttemptAt());
        assertFalse(row.getProcessed() != null && row.getProcessed());
        verify(repository).save(row);
    }

    @Test
    void shouldRecordPermanentFirstFailureAsTerminalRightAway() {

        recorder().recordFailure(7L, new SenderFailure("WEBHOOK", new PermanentNotificationException("bad")));

        ArgumentCaptor<FailedNotification> saved = ArgumentCaptor.forClass(FailedNotification.class);
        verify(repository).save(saved.capture());

        assertEquals(NotificationRetryPolicy.MAX_ATTEMPTS, saved.getValue().getAttempts());
        assertNull(saved.getValue().getNextAttemptAt());
    }

    @Test
    void shouldReleaseTheLeaseWhenRecordingARetryFailure() {

        FailedNotification row = new FailedNotification();
        row.setAttempts(1);
        row.setLockedUntil(Instant.now().plusSeconds(120));
        when(repository.findById(5L)).thenReturn(Optional.of(row));

        recorder().recordRetryFailure(5L, new RuntimeException("down"));

        assertNull(row.getLockedUntil());
    }

    @Test
    void shouldNotOverwriteARowThatWasProcessedMeanwhile() {

        FailedNotification row = new FailedNotification();
        row.setAttempts(1);
        row.setProcessed(true);
        when(repository.findById(5L)).thenReturn(Optional.of(row));

        recorder().recordRetryFailure(5L, new RuntimeException("down"));

        assertEquals(1, row.getAttempts());
        assertTrue(row.getProcessed());
        verify(repository, never()).save(any());
    }
}
