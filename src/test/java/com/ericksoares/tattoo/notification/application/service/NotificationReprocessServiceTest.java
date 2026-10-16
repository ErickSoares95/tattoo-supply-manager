package com.ericksoares.tattoo.notification.application.service;

import com.ericksoares.tattoo.notification.application.dto.NotificationContext;
import com.ericksoares.tattoo.notification.application.dto.SenderFailure;
import com.ericksoares.tattoo.notification.domain.entity.FailedNotification;
import com.ericksoares.tattoo.notification.domain.repository.FailedNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationReprocessServiceTest {

    @Mock
    private FailedNotificationRepository repository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private FailedNotificationRecorder recorder;

    private FailedNotification failureOn(String senderName) {
        FailedNotification failure = new FailedNotification();
        failure.setId(1L);
        failure.setOrderId(10L);
        failure.setSenderName(senderName);
        failure.setProcessed(false);
        return failure;
    }

    private void givenPending(FailedNotification... failures) {
        when(repository.findRetryable(eq(NotificationRetryPolicy.MAX_ATTEMPTS), any(Instant.class), eq(0L), any()))
                .thenReturn(List.of(failures));
    }

    private void givenClaimable(Long id) {
        when(repository.claim(eq(id), any(Instant.class), any(Instant.class))).thenReturn(1);
    }

    private NotificationReprocessService service() {
        return new NotificationReprocessService(repository, notificationService, recorder);
    }

    @Test
    void shouldMarkAsProcessedWhenResendSucceeds() {

        FailedNotification failure = failureOn("WEBHOOK");
        givenPending(failure);
        givenClaimable(1L);

        when(notificationService.notifyOrderRegistered(any(NotificationContext.class), eq("WEBHOOK")))
                .thenReturn(Optional.empty());

        service().reprocessAll();

        verify(repository).markProcessed(1L);
        verify(repository, never()).save(any());
        verifyNoInteractions(recorder);
    }

    @Test
    void shouldResendOnlyThroughTheSenderThatFailed() {

        givenPending(failureOn("WHATSAPP"));
        givenClaimable(1L);

        when(notificationService.notifyOrderRegistered(any(NotificationContext.class), eq("WHATSAPP")))
                .thenReturn(Optional.empty());

        service().reprocessAll();

        verify(notificationService).notifyOrderRegistered(new NotificationContext(10L), "WHATSAPP");
        verify(notificationService, never()).notifyOrderRegistered(any(NotificationContext.class));
    }

    @Test
    void shouldKeepUnprocessedAndRecordAttemptWhenResendFailsAgain() {

        givenPending(failureOn("WEBHOOK"));
        givenClaimable(1L);

        RuntimeException error = new RuntimeException("still down");
        when(notificationService.notifyOrderRegistered(any(NotificationContext.class), eq("WEBHOOK")))
                .thenReturn(Optional.of(new SenderFailure("WEBHOOK", error)));

        service().reprocessAll();

        verify(recorder).recordRetryFailure(1L, error);
        verify(repository, never()).markProcessed(anyLong());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldReplayLegacyRowThroughAllSendersAndReplaceItWithPerSenderRows() {

        FailedNotification legacy = failureOn(null);
        givenPending(legacy);
        givenClaimable(1L);

        SenderFailure webhookFailure = new SenderFailure("WEBHOOK", new RuntimeException("down"));
        when(notificationService.notifyOrderRegistered(new NotificationContext(10L)))
                .thenReturn(List.of(webhookFailure));

        service().reprocessAll();

        verify(repository).markProcessed(1L);
        verify(recorder).recordFailure(10L, webhookFailure);
    }

    @Test
    void shouldNotReplayLegacyRowWhenAnotherWorkerClaimedIt() {

        givenPending(failureOn(null));
        when(repository.claim(eq(1L), any(Instant.class), any(Instant.class))).thenReturn(0);

        service().reprocessAll();

        verifyNoInteractions(notificationService, recorder);
        verify(repository, never()).markProcessed(anyLong());
    }

    @Test
    void shouldNotSendWhenClaimIsLost() {

        givenPending(failureOn("WEBHOOK"));
        when(repository.claim(eq(1L), any(Instant.class), any(Instant.class))).thenReturn(0);

        service().reprocessAll();

        verifyNoInteractions(notificationService, recorder);
        verify(repository, never()).markProcessed(anyLong());
    }

    @Test
    void shouldPageThroughBatchesUsingTheLastSeenId() {

        FailedNotification first = failureOn("EMAIL");
        FailedNotification second = failureOn("EMAIL");
        second.setId(2L);

        when(repository.findRetryable(eq(NotificationRetryPolicy.MAX_ATTEMPTS), any(Instant.class), eq(0L), any()))
                .thenReturn(List.of(first));
        when(repository.findRetryable(eq(NotificationRetryPolicy.MAX_ATTEMPTS), any(Instant.class), eq(1L), any()))
                .thenReturn(List.of(second));
        givenClaimable(1L);
        givenClaimable(2L);

        when(notificationService.notifyOrderRegistered(any(NotificationContext.class), anyString()))
                .thenReturn(Optional.empty());

        service().reprocessAll();

        verify(repository).markProcessed(1L);
        verify(repository).markProcessed(2L);
    }

    @Test
    void shouldReprocessOnlyDueFailuresWhenRunBySchedule() {

        FailedNotification due = failureOn("EMAIL");

        when(repository.findDue(eq(NotificationRetryPolicy.MAX_ATTEMPTS), any(Instant.class), eq(0L), any()))
                .thenReturn(List.of(due));
        givenClaimable(1L);
        when(notificationService.notifyOrderRegistered(any(NotificationContext.class), eq("EMAIL")))
                .thenReturn(Optional.empty());

        service().reprocessDue();

        verify(repository).markProcessed(1L);
        verify(repository, never()).findRetryable(anyInt(), any(Instant.class), anyLong(), any());
    }
}
