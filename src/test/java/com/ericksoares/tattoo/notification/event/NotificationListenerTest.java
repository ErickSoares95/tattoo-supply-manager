package com.ericksoares.tattoo.notification.event;

import com.ericksoares.tattoo.notification.application.dto.NotificationContext;
import com.ericksoares.tattoo.notification.application.dto.SenderFailure;
import com.ericksoares.tattoo.notification.application.listener.NotificationSender;
import com.ericksoares.tattoo.notification.application.service.FailedNotificationRecorder;
import com.ericksoares.tattoo.notification.application.service.NotificationSenderExecutor;
import com.ericksoares.tattoo.notification.application.service.NotificationService;
import com.ericksoares.tattoo.order.application.dto.OrderItemData;
import com.ericksoares.tattoo.order.domain.event.OrderRegisteredEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    private static final Executor DIRECT = Runnable::run;

    @Mock
    private NotificationSender email;

    @Mock
    private NotificationSender webhook;

    @Mock
    private NotificationSenderExecutor executor;

    @Mock
    private FailedNotificationRecorder recorder;

    private OrderRegisteredEvent orderWithThreeItems() {
        return new OrderRegisteredEvent(
                7L,
                List.of(
                        new OrderItemData(1L, "Tinta Black", 2),
                        new OrderItemData(2L, "Agulha 5RL", 1),
                        new OrderItemData(3L, "Luva", 4)
                ),
                LocalDateTime.now()
        );
    }

    @Test
    void shouldNotifyEachSenderOnceForAnOrderWithThreeItems() {

        NotificationService service = new NotificationService(List.of(email, webhook), executor);
        NotificationListener listener = new NotificationListener(service, recorder, DIRECT);

        listener.handle(orderWithThreeItems());

        verify(executor, times(1)).send(email, 7L);
        verify(executor, times(1)).send(webhook, 7L);
        verifyNoInteractions(recorder);
    }

    @Test
    void shouldPersistOneFailurePerFailedSenderOnly() {

        NotificationService service = new NotificationService(List.of(email, webhook), executor);
        NotificationListener listener = new NotificationListener(service, recorder, DIRECT);

        when(webhook.name()).thenReturn("WEBHOOK");
        lenient().when(email.name()).thenReturn("EMAIL");
        doNothing().when(executor).send(email, 7L);
        doThrow(new RuntimeException("webhook down")).when(executor).send(webhook, 7L);

        listener.handle(orderWithThreeItems());

        verify(recorder, times(1)).recordFailure(eq(7L), any(SenderFailure.class));
        verify(recorder).recordFailure(eq(7L), argThat(f -> f.senderName().equals("WEBHOOK")));
        verify(executor, times(1)).send(eq(email), anyLong());
    }

    @Test
    void shouldCallNotificationServiceOncePerOrder() {

        NotificationService service = mock(NotificationService.class);
        NotificationListener listener = new NotificationListener(service, recorder, DIRECT);

        when(service.notifyOrderRegistered(any(NotificationContext.class))).thenReturn(List.of());

        listener.handle(orderWithThreeItems());

        verify(service, times(1)).notifyOrderRegistered(new NotificationContext(7L));
    }
}
