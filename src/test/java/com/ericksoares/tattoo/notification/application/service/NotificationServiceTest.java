package com.ericksoares.tattoo.notification.application.service;

import com.ericksoares.tattoo.notification.application.dto.NotificationContext;
import com.ericksoares.tattoo.notification.application.dto.SenderFailure;
import com.ericksoares.tattoo.notification.application.listener.NotificationSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationSender workingSender;

    @Mock
    private NotificationSender failingSender;

    @Mock
    private NotificationSenderExecutor executor;

    @Test
    void shouldReturnNoFailuresWhenAllSendersSucceed() {

        NotificationService service = new NotificationService(
                List.of(workingSender, failingSender),
                executor
        );

        NotificationContext context = new NotificationContext(1L);

        List<SenderFailure> result = service.notifyOrderRegistered(context);

        assertTrue(result.isEmpty());

        verify(executor).send(workingSender, 1L);
        verify(executor).send(failingSender, 1L);
    }

    @Test
    void shouldNotRetrySuccessfulSenderWhenAnotherSenderFails() {

        NotificationService service = new NotificationService(
                List.of(workingSender, failingSender),
                executor
        );

        NotificationContext context = new NotificationContext(1L);

        lenient().when(workingSender.name()).thenReturn("EMAIL");
        when(failingSender.name()).thenReturn("WEBHOOK");

        doNothing()
                .when(executor).send(workingSender, 1L);

        doThrow(new RuntimeException("webhook indisponivel"))
                .when(executor).send(failingSender, 1L);

        List<SenderFailure> result = service.notifyOrderRegistered(context);

        assertEquals(1, result.size());
        assertEquals("WEBHOOK", result.get(0).senderName());

        verify(executor, times(1)).send(workingSender, 1L);
        verify(executor, times(1)).send(failingSender, 1L);
    }

    @Test
    void shouldSendOnlyThroughTheRequestedSender() {

        NotificationService service = new NotificationService(
                List.of(workingSender, failingSender),
                executor
        );

        when(workingSender.name()).thenReturn("EMAIL");
        when(failingSender.name()).thenReturn("WEBHOOK");

        Optional<SenderFailure> result =
                service.notifyOrderRegistered(new NotificationContext(1L), "WEBHOOK");

        assertTrue(result.isEmpty());

        verify(executor).send(failingSender, 1L);
        verify(executor, never()).send(eq(workingSender), anyLong());
    }

    @Test
    void shouldReportFailureWhenRequestedSenderIsUnknown() {

        NotificationService service = new NotificationService(
                List.of(workingSender),
                executor
        );

        when(workingSender.name()).thenReturn("EMAIL");

        Optional<SenderFailure> result =
                service.notifyOrderRegistered(new NotificationContext(1L), "SMS");

        assertTrue(result.isPresent());
        assertEquals("SMS", result.get().senderName());

        verifyNoInteractions(executor);
    }

    @Test
    void shouldNotifyPaymentConfirmedToAllSenders() {

        NotificationService service = new NotificationService(
                List.of(workingSender, failingSender),
                executor
        );

        List<SenderFailure> result = service.notifyPaymentConfirmed(1L);

        assertTrue(result.isEmpty());

        verify(executor).sendPaymentConfirmed(workingSender, 1L);
        verify(executor).sendPaymentConfirmed(failingSender, 1L);
    }

    @Test
    void shouldReportOnlyTheSenderThatFailedToNotifyPaymentRejected() {

        NotificationService service = new NotificationService(
                List.of(workingSender, failingSender),
                executor
        );

        lenient().when(workingSender.name()).thenReturn("EMAIL");
        when(failingSender.name()).thenReturn("WEBHOOK");

        RuntimeException error = new RuntimeException("webhook indisponivel");
        lenient().doThrow(error).when(executor).sendPaymentRejected(failingSender, 1L);

        List<SenderFailure> result = service.notifyPaymentRejected(1L);

        assertEquals(1, result.size());
        assertEquals("WEBHOOK", result.get(0).senderName());
        assertSame(error, result.get(0).error());

        verify(executor, times(1)).sendPaymentRejected(workingSender, 1L);
        verify(executor, times(1)).sendPaymentRejected(failingSender, 1L);
    }

    @Test
    void shouldResendPaymentConfirmedOnlyThroughTheNamedSender() {

        NotificationService service = new NotificationService(
                List.of(workingSender, failingSender),
                executor
        );

        lenient().when(workingSender.name()).thenReturn("EMAIL");
        when(failingSender.name()).thenReturn("WEBHOOK");

        Optional<SenderFailure> result = service.notifyPaymentConfirmed(1L, "WEBHOOK");

        assertTrue(result.isEmpty());

        verify(executor).sendPaymentConfirmed(failingSender, 1L);
        verify(executor, never()).sendPaymentConfirmed(eq(workingSender), anyLong());
    }

    @Test
    void shouldReturnFailureWhenTargetedPaymentRejectedResendFails() {

        NotificationService service = new NotificationService(
                List.of(workingSender, failingSender),
                executor
        );

        lenient().when(workingSender.name()).thenReturn("EMAIL");
        when(failingSender.name()).thenReturn("WEBHOOK");

        doThrow(new RuntimeException("still down")).when(executor).sendPaymentRejected(failingSender, 1L);

        Optional<SenderFailure> result = service.notifyPaymentRejected(1L, "WEBHOOK");

        assertTrue(result.isPresent());
        assertEquals("WEBHOOK", result.get().senderName());
        verify(executor, never()).sendPaymentRejected(eq(workingSender), anyLong());
    }

    @Test
    void shouldReturnPermanentFailureWhenPaymentSenderIsUnknown() {

        NotificationService service = new NotificationService(List.of(workingSender), executor);

        when(workingSender.name()).thenReturn("EMAIL");

        Optional<SenderFailure> result = service.notifyPaymentConfirmed(1L, "SMS");

        assertTrue(result.isPresent());
        assertInstanceOf(com.ericksoares.tattoo.notification.domain.exception.PermanentNotificationException.class,
                result.get().error());
    }
}
