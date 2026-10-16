package com.ericksoares.tattoo.notification;

import com.ericksoares.tattoo.notification.sender.NotificationSender;
import com.ericksoares.tattoo.payment.PaymentStatus;
import com.ericksoares.tattoo.payment.PaymentProcessedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationListenerTest {

    private static final Executor DIRECT = Runnable::run;
    private static final UUID EVENT_ID = UUID.randomUUID();

    @Mock
    private NotificationSender email;

    @Mock
    private NotificationSender webhook;

    @Mock
    private NotificationSenderExecutor executor;

    @Mock
    private FailedPaymentNotificationRecorder recorder;

    private PaymentProcessedEvent event(PaymentStatus status) {
        return new PaymentProcessedEvent(EVENT_ID, 7L, status, BigDecimal.TEN, LocalDateTime.now());
    }

    @Test
    void shouldNotifyEachSenderOnceForAnApprovedPayment() {

        NotificationService service = mock(NotificationService.class);
        PaymentNotificationListener listener = new PaymentNotificationListener(service, recorder, DIRECT);

        when(service.notifyPaymentConfirmed(7L)).thenReturn(List.of());

        listener.handle(event(PaymentStatus.APPROVED));

        verify(service, times(1)).notifyPaymentConfirmed(7L);
        verify(service, never()).notifyPaymentRejected(anyLong());
        verifyNoInteractions(recorder);
    }

    @Test
    void shouldCallRejectedNotificationForARejectedPayment() {

        NotificationService service = mock(NotificationService.class);
        PaymentNotificationListener listener = new PaymentNotificationListener(service, recorder, DIRECT);

        when(service.notifyPaymentRejected(7L)).thenReturn(List.of());

        listener.handle(event(PaymentStatus.REJECTED));

        verify(service, times(1)).notifyPaymentRejected(7L);
        verify(service, never()).notifyPaymentConfirmed(anyLong());
    }

    @Test
    void shouldPersistOneFailurePerFailedSenderOnly() {

        NotificationService service = new NotificationService(List.of(email, webhook), executor);
        PaymentNotificationListener listener = new PaymentNotificationListener(service, recorder, DIRECT);

        when(webhook.name()).thenReturn("WEBHOOK");
        lenient().when(email.name()).thenReturn("EMAIL");
        lenient().doNothing().when(executor).sendPaymentConfirmed(email, 7L);
        doThrow(new RuntimeException("webhook down")).when(executor).sendPaymentConfirmed(webhook, 7L);

        listener.handle(event(PaymentStatus.APPROVED));

        verify(recorder, times(1)).recordFailure(
                eq(EVENT_ID), eq(7L), eq("APPROVED"), eq(BigDecimal.TEN), any(SenderFailure.class));
        verify(recorder).recordFailure(
                eq(EVENT_ID), eq(7L), eq("APPROVED"), eq(BigDecimal.TEN),
                argThat(f -> f.senderName().equals("WEBHOOK")));
    }

    @Test
    void shouldHandTheWorkToTheExecutor() {

        NotificationService service = mock(NotificationService.class);
        Executor notificationExecutor = mock(Executor.class);
        PaymentNotificationListener listener = new PaymentNotificationListener(service, recorder, notificationExecutor);

        listener.handle(event(PaymentStatus.APPROVED));

        verify(notificationExecutor).execute(any(PaymentNotificationTask.class));
        verifyNoInteractions(service, recorder);
    }
}
