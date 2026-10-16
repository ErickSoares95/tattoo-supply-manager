package com.ericksoares.tattoo.notification.application.service;

import com.ericksoares.tattoo.notification.application.dto.SenderFailure;
import com.ericksoares.tattoo.notification.domain.entity.FailedPaymentNotification;
import com.ericksoares.tattoo.notification.domain.exception.PermanentNotificationException;
import com.ericksoares.tattoo.notification.domain.repository.FailedPaymentNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationReprocessServiceTest {

    @Mock
    private FailedPaymentNotificationRepository repository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private FailedPaymentNotificationRecorder recorder;

    private FailedPaymentNotification failure(String status, String senderName) {
        FailedPaymentNotification failure = new FailedPaymentNotification();
        failure.setId(1L);
        failure.setOrderId(10L);
        failure.setStatus(status);
        failure.setSenderName(senderName);
        failure.setAmount(BigDecimal.TEN);
        failure.setEventId(UUID.randomUUID());
        failure.setProcessed(false);
        return failure;
    }

    private void givenDue(FailedPaymentNotification... failures) {
        when(repository.findDue(eq(NotificationRetryPolicy.MAX_ATTEMPTS), any(Instant.class), eq(0L), any()))
                .thenReturn(List.of(failures));
    }

    private void givenClaimable() {
        when(repository.claim(eq(1L), any(Instant.class), any(Instant.class))).thenReturn(1);
    }

    private PaymentNotificationReprocessService service() {
        return new PaymentNotificationReprocessService(repository, notificationService, recorder);
    }

    @Test
    void shouldResendApprovedPaymentOnlyThroughTheFailedSender() {

        givenDue(failure("APPROVED", "WEBHOOK"));
        givenClaimable();
        when(notificationService.notifyPaymentConfirmed(10L, "WEBHOOK")).thenReturn(Optional.empty());

        service().reprocessDue();

        verify(notificationService).notifyPaymentConfirmed(10L, "WEBHOOK");
        verify(notificationService, never()).notifyPaymentRejected(anyLong(), any());
        verify(repository).markProcessed(1L);
        verifyNoInteractions(recorder);
    }

    @Test
    void shouldResendRejectedPaymentThroughRejectedChannel() {

        givenDue(failure("REJECTED", "EMAIL"));
        givenClaimable();
        when(notificationService.notifyPaymentRejected(10L, "EMAIL")).thenReturn(Optional.empty());

        service().reprocessDue();

        verify(notificationService).notifyPaymentRejected(10L, "EMAIL");
        verify(notificationService, never()).notifyPaymentConfirmed(anyLong(), any());
        verify(repository).markProcessed(1L);
    }

    @Test
    void shouldRecordRetryFailureWhenResendFailsAgain() {

        givenDue(failure("APPROVED", "WEBHOOK"));
        givenClaimable();

        RuntimeException error = new RuntimeException("still down");
        when(notificationService.notifyPaymentConfirmed(10L, "WEBHOOK"))
                .thenReturn(Optional.of(new SenderFailure("WEBHOOK", error)));

        service().reprocessDue();

        verify(recorder).recordRetryFailure(1L, error);
        verify(repository, never()).markProcessed(anyLong());
    }

    @Test
    void shouldNotSendWhenClaimIsLost() {

        givenDue(failure("APPROVED", "WEBHOOK"));
        when(repository.claim(eq(1L), any(Instant.class), any(Instant.class))).thenReturn(0);

        service().reprocessDue();

        verifyNoInteractions(notificationService, recorder);
        verify(repository, never()).markProcessed(anyLong());
    }

    @Test
    void shouldReplayLegacyRowThroughAllSendersAndReplaceItWithPerSenderRows() {

        FailedPaymentNotification legacy = failure("APPROVED", null);
        givenDue(legacy);
        givenClaimable();

        SenderFailure webhookFailure = new SenderFailure("WEBHOOK", new RuntimeException("down"));
        when(notificationService.notifyPaymentConfirmed(10L)).thenReturn(List.of(webhookFailure));

        service().reprocessDue();

        verify(recorder).recordFailure(legacy.getEventId(), 10L, "APPROVED", BigDecimal.TEN, webhookFailure);
        verify(repository).markProcessed(1L);
    }

    @Test
    void shouldParkRowWithUnsupportedStatusAsPermanentFailure() {

        givenDue(failure("PENDING", "WEBHOOK"));
        givenClaimable();

        service().reprocessDue();

        verify(recorder).recordRetryFailure(eq(1L), any(PermanentNotificationException.class));
        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldReprocessAllRegardlessOfBackoffWhenRunManually() {

        FailedPaymentNotification pending = failure("APPROVED", "WEBHOOK");
        when(repository.findRetryable(eq(NotificationRetryPolicy.MAX_ATTEMPTS), any(Instant.class), eq(0L), any()))
                .thenReturn(List.of(pending));
        givenClaimable();
        when(notificationService.notifyPaymentConfirmed(10L, "WEBHOOK")).thenReturn(Optional.empty());

        service().reprocessAll();

        verify(repository).markProcessed(1L);
        verify(repository, never()).findDue(anyInt(), any(Instant.class), anyLong(), any());
    }
}
