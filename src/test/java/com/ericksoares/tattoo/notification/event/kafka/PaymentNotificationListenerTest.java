package com.ericksoares.tattoo.notification.event.kafka;

import com.ericksoares.tattoo.notification.application.dto.SenderFailure;
import com.ericksoares.tattoo.notification.application.service.FailedPaymentNotificationRecorder;
import com.ericksoares.tattoo.notification.application.service.NotificationService;
import com.ericksoares.tattoo.notification.domain.entity.FailedPaymentNotification;
import com.ericksoares.tattoo.notification.domain.entity.ProcessedPaymentEvent;
import com.ericksoares.tattoo.notification.domain.repository.FailedPaymentNotificationRepository;
import com.ericksoares.tattoo.notification.domain.repository.ProcessedPaymentEventRepository;
import com.ericksoares.tattoo.payment.domain.entity.PaymentStatus;
import com.ericksoares.tattoo.payment.domain.event.PaymentProcessedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private FailedPaymentNotificationRecorder recorder;

    @Mock
    private ProcessedPaymentEventRepository processedRepository;

    @Mock
    private FailedPaymentNotificationRepository failedRepository;

    @InjectMocks
    private PaymentNotificationListener listener;

    @Test
    void shouldNotifyPaymentConfirmedWhenStatusIsApproved() {

        PaymentProcessedEvent event = new PaymentProcessedEvent(
                UUID.randomUUID(), 1L, PaymentStatus.APPROVED, BigDecimal.valueOf(100), LocalDateTime.now()
        );

        when(notificationService.notifyPaymentConfirmed(1L))
                .thenReturn(List.of());

        listener.handle(event);

        verify(notificationService).notifyPaymentConfirmed(1L);
        verify(notificationService, never()).notifyPaymentRejected(any());
        verifyNoInteractions(recorder);
    }

    @Test
    void shouldNotifyPaymentRejectedWhenStatusIsRejected() {

        PaymentProcessedEvent event = new PaymentProcessedEvent(
                UUID.randomUUID(), 1L, PaymentStatus.REJECTED, BigDecimal.valueOf(50), LocalDateTime.now()
        );

        when(notificationService.notifyPaymentRejected(1L))
                .thenReturn(List.of());

        listener.handle(event);

        verify(notificationService).notifyPaymentRejected(1L);
        verify(notificationService, never()).notifyPaymentConfirmed(any());
        verifyNoInteractions(recorder);
    }

    @Test
    void shouldPersistOneRowPerFailedSender() {

        UUID eventId = UUID.randomUUID();
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                eventId, 1L, PaymentStatus.APPROVED, BigDecimal.valueOf(100), LocalDateTime.now()
        );

        SenderFailure webhook = new SenderFailure("WEBHOOK", new RuntimeException("down"));
        SenderFailure whatsapp = new SenderFailure("WHATSAPP", new RuntimeException("down"));

        when(notificationService.notifyPaymentConfirmed(1L))
                .thenReturn(List.of(webhook, whatsapp));

        listener.handle(event);

        verify(recorder).recordFailure(eventId, 1L, "APPROVED", BigDecimal.valueOf(100), webhook);
        verify(recorder).recordFailure(eventId, 1L, "APPROVED", BigDecimal.valueOf(100), whatsapp);
    }

    @Test
    void shouldReleaseClaimAndPropagateWhenRecordingTheFailureFails() {

        UUID eventId = UUID.randomUUID();
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                eventId, 1L, PaymentStatus.REJECTED, BigDecimal.valueOf(50), LocalDateTime.now()
        );

        SenderFailure webhook = new SenderFailure("WEBHOOK", new RuntimeException("down"));
        RuntimeException dbDown = new RuntimeException("db down");

        when(notificationService.notifyPaymentRejected(1L)).thenReturn(List.of(webhook));
        when(recorder.recordFailure(eventId, 1L, "REJECTED", BigDecimal.valueOf(50), webhook))
                .thenThrow(dbDown);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> listener.handle(event));

        assertSame(dbDown, thrown);
        verify(processedRepository).releaseClaim(eventId);
    }

    @Test
    void shouldPropagateOriginalExceptionWhenReleasingTheClaimFailsToo() {

        UUID eventId = UUID.randomUUID();
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                eventId, 1L, PaymentStatus.APPROVED, BigDecimal.valueOf(50), LocalDateTime.now()
        );

        SenderFailure webhook = new SenderFailure("WEBHOOK", new RuntimeException("down"));
        RuntimeException dbDown = new RuntimeException("db down");
        RuntimeException releaseFailed = new RuntimeException("db still down");

        when(notificationService.notifyPaymentConfirmed(1L)).thenReturn(List.of(webhook));
        when(recorder.recordFailure(eventId, 1L, "APPROVED", BigDecimal.valueOf(50), webhook))
                .thenThrow(dbDown);
        when(processedRepository.releaseClaim(eventId)).thenThrow(releaseFailed);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> listener.handle(event));

        assertSame(dbDown, thrown);
        assertArrayEquals(new Throwable[]{releaseFailed}, thrown.getSuppressed());
    }

    @Test
    void shouldNotDuplicateRowsOnRedeliveryAfterPartialFailure() {

        UUID eventId = UUID.randomUUID();
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                eventId, 1L, PaymentStatus.APPROVED, BigDecimal.valueOf(100), LocalDateTime.now()
        );

        SenderFailure webhook = new SenderFailure("WEBHOOK", new RuntimeException("down"));
        SenderFailure whatsapp = new SenderFailure("WHATSAPP", new RuntimeException("down"));

        when(notificationService.notifyPaymentConfirmed(1L)).thenReturn(List.of(webhook, whatsapp));

        // First delivery: WEBHOOK row is saved, WHATSAPP fails -> claim released, exception propagated.
        when(recorder.recordFailure(eventId, 1L, "APPROVED", BigDecimal.valueOf(100), webhook))
                .thenReturn(new FailedPaymentNotification());
        AtomicInteger whatsappCalls = new AtomicInteger();
        when(recorder.recordFailure(eventId, 1L, "APPROVED", BigDecimal.valueOf(100), whatsapp))
                .thenAnswer(invocation -> {
                    if (whatsappCalls.incrementAndGet() == 1) {
                        throw new RuntimeException("db down");
                    }
                    return new FailedPaymentNotification();
                });

        assertThrows(RuntimeException.class, () -> listener.handle(event));
        verify(recorder, times(1)).recordFailure(eventId, 1L, "APPROVED", BigDecimal.valueOf(100), webhook);
        verify(processedRepository).releaseClaim(eventId);

        // Redelivery: WEBHOOK already has a row, so only WHATSAPP is recorded.
        when(failedRepository.existsByEventIdAndSenderName(eventId, "WEBHOOK")).thenReturn(true);

        listener.handle(event);

        verify(recorder, times(1)).recordFailure(eventId, 1L, "APPROVED", BigDecimal.valueOf(100), webhook);
        verify(recorder, times(2)).recordFailure(eventId, 1L, "APPROVED", BigDecimal.valueOf(100), whatsapp);
    }

    @Test
    void shouldSkipProcessingWhenEventWasAlreadyClaimed() {

        PaymentProcessedEvent event = new PaymentProcessedEvent(
                UUID.randomUUID(), 1L, PaymentStatus.APPROVED, BigDecimal.valueOf(100), LocalDateTime.now()
        );

        when(processedRepository.saveAndFlush(any(ProcessedPaymentEvent.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate event_id"));

        listener.handle(event);

        verify(notificationService, never()).notifyPaymentConfirmed(any());
        verify(notificationService, never()).notifyPaymentRejected(any());
        verifyNoInteractions(recorder);
    }
}
