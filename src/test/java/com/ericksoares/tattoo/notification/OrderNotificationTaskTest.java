package com.ericksoares.tattoo.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderNotificationTaskTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private FailedNotificationRecorder recorder;

    @Test
    void shouldPersistOnePendingFailurePerSenderWhenRejected() {

        when(notificationService.senderNames()).thenReturn(List.of("EMAIL", "WHATSAPP", "WEBHOOK"));

        OrderNotificationTask task = new OrderNotificationTask(7L, notificationService, recorder);

        task.onRejected();

        verify(recorder).recordRejected(7L, "EMAIL");
        verify(recorder).recordRejected(7L, "WHATSAPP");
        verify(recorder).recordRejected(7L, "WEBHOOK");
        verify(notificationService, never()).notifyOrderRegistered(any());
    }

    @Test
    void shouldNotThrowWhenPersistingTheRejectionFails() {

        when(notificationService.senderNames()).thenReturn(List.of("EMAIL"));
        doThrow(new RuntimeException("db down")).when(recorder).recordRejected(7L, "EMAIL");

        OrderNotificationTask task = new OrderNotificationTask(7L, notificationService, recorder);

        assertDoesNotThrow(task::onRejected);
    }
}
