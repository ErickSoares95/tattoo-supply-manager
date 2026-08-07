package com.ericksoares.tattoo.notification.application.service;

import com.ericksoares.tattoo.notification.application.dto.NotificationContext;
import com.ericksoares.tattoo.notification.application.listener.NotificationSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationSender workingSender;

    @Mock
    private NotificationSender failingSender;

    @Mock
    private NotificationSenderExecutor executor;

    @Test
    void shouldReturnTrueWhenAllSendersSucceed() {

        NotificationService service = new NotificationService(
                List.of(workingSender, failingSender),
                executor
        );

        NotificationContext context = new NotificationContext(1L, "Tinta Black", 2);

        boolean result = service.notifyOrderRegistered(context);

        assertTrue(result);

        verify(executor).send(workingSender, 1L);
        verify(executor).send(failingSender, 1L);
    }

    @Test
    void shouldNotRetrySuccessfulSenderWhenAnotherSenderFails() {

        NotificationService service = new NotificationService(
                List.of(workingSender, failingSender),
                executor
        );

        NotificationContext context = new NotificationContext(1L, "Tinta Black", 2);

        doNothing()
                .when(executor).send(workingSender, 1L);

        doThrow(new RuntimeException("webhook indisponivel"))
                .when(executor).send(failingSender, 1L);

        boolean result = service.notifyOrderRegistered(context);

        assertFalse(result);

        verify(executor, times(1)).send(workingSender, 1L);
        verify(executor, times(1)).send(failingSender, 1L);
    }

    @Test
    void shouldNotifyPaymentConfirmedToAllSenders() {

        NotificationService service = new NotificationService(
                List.of(workingSender, failingSender),
                executor
        );

        boolean result = service.notifyPaymentConfirmed(1L);

        assertTrue(result);

        verify(executor).sendPaymentConfirmed(workingSender, 1L);
        verify(executor).sendPaymentConfirmed(failingSender, 1L);
    }

    @Test
    void shouldReturnFalseWhenAnySenderFailsToNotifyPaymentRejected() {

        NotificationService service = new NotificationService(
                List.of(workingSender, failingSender),
                executor
        );

        doThrow(new RuntimeException("webhook indisponivel"))
                .when(executor).sendPaymentRejected(failingSender, 1L);

        boolean result = service.notifyPaymentRejected(1L);

        assertFalse(result);

        verify(executor, times(1)).sendPaymentRejected(workingSender, 1L);
        verify(executor, times(1)).sendPaymentRejected(failingSender, 1L);
    }
}
