package com.ericksoares.tattoo.notification.event;

import com.ericksoares.tattoo.notification.application.dto.SenderFailure;
import com.ericksoares.tattoo.notification.application.service.FailedPaymentNotificationRecorder;
import com.ericksoares.tattoo.notification.application.service.NotificationService;
import com.ericksoares.tattoo.payment.domain.entity.PaymentStatus;
import com.ericksoares.tattoo.payment.domain.event.PaymentProcessedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationTaskTest {

    private static final UUID EVENT_ID = UUID.randomUUID();

    @Mock
    private NotificationService notificationService;

    @Mock
    private FailedPaymentNotificationRecorder recorder;

    private PaymentNotificationTask task(PaymentStatus status) {
        return new PaymentNotificationTask(
                new PaymentProcessedEvent(EVENT_ID, 7L, status, BigDecimal.TEN, LocalDateTime.now()),
                notificationService,
                recorder
        );
    }

    @Test
    void shouldNotifyConfirmedWhenApproved() {

        when(notificationService.notifyPaymentConfirmed(7L)).thenReturn(List.of());

        task(PaymentStatus.APPROVED).run();

        verify(notificationService).notifyPaymentConfirmed(7L);
        verify(notificationService, never()).notifyPaymentRejected(anyLong());
        verifyNoInteractions(recorder);
    }

    @Test
    void shouldNotifyRejectedWhenRejected() {

        when(notificationService.notifyPaymentRejected(7L)).thenReturn(List.of());

        task(PaymentStatus.REJECTED).run();

        verify(notificationService).notifyPaymentRejected(7L);
        verify(notificationService, never()).notifyPaymentConfirmed(anyLong());
        verifyNoInteractions(recorder);
    }

    @Test
    void shouldNotNotifyWhenPending() {

        task(PaymentStatus.PENDING).run();

        verify(notificationService, never()).notifyPaymentConfirmed(anyLong());
        verify(notificationService, never()).notifyPaymentRejected(anyLong());
        verifyNoInteractions(recorder);
    }

    @Test
    void shouldPersistOneFailurePerFailedSender() {

        SenderFailure webhook = new SenderFailure("WEBHOOK", new RuntimeException("down"));
        SenderFailure whatsapp = new SenderFailure("WHATSAPP", new RuntimeException("down"));
        when(notificationService.notifyPaymentConfirmed(7L)).thenReturn(List.of(webhook, whatsapp));

        task(PaymentStatus.APPROVED).run();

        verify(recorder).recordFailure(EVENT_ID, 7L, "APPROVED", BigDecimal.TEN, webhook);
        verify(recorder).recordFailure(EVENT_ID, 7L, "APPROVED", BigDecimal.TEN, whatsapp);
        verifyNoMoreInteractions(recorder);
    }

    @Test
    void shouldNotThrowWhenNotifyingFails() {

        when(notificationService.notifyPaymentRejected(7L)).thenThrow(new RuntimeException("boom"));

        assertDoesNotThrow(() -> task(PaymentStatus.REJECTED).run());
    }

    @Test
    void shouldPersistOnePendingFailurePerSenderWhenRejected() {

        when(notificationService.senderNames()).thenReturn(List.of("EMAIL", "WHATSAPP", "WEBHOOK"));

        task(PaymentStatus.APPROVED).onRejected();

        verify(recorder).recordRejected(EVENT_ID, 7L, "APPROVED", BigDecimal.TEN, "EMAIL");
        verify(recorder).recordRejected(EVENT_ID, 7L, "APPROVED", BigDecimal.TEN, "WHATSAPP");
        verify(recorder).recordRejected(EVENT_ID, 7L, "APPROVED", BigDecimal.TEN, "WEBHOOK");
        verify(notificationService, never()).notifyPaymentConfirmed(anyLong());
    }

    @Test
    void shouldNotThrowWhenPersistingTheRejectionFails() {

        when(notificationService.senderNames()).thenReturn(List.of("EMAIL"));
        doThrow(new RuntimeException("db down"))
                .when(recorder).recordRejected(EVENT_ID, 7L, "APPROVED", BigDecimal.TEN, "EMAIL");

        assertDoesNotThrow(() -> task(PaymentStatus.APPROVED).onRejected());
    }
}
