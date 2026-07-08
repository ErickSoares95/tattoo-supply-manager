package com.ericksoares.tattoo.notification.application.service;

import com.ericksoares.tattoo.notification.application.dto.NotificationContext;
import com.ericksoares.tattoo.notification.domain.entity.FailedNotification;
import com.ericksoares.tattoo.notification.domain.repository.FailedNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationReprocessServiceTest {

    @Mock
    private FailedNotificationRepository repository;

    @Mock
    private NotificationService notificationService;

    @Test
    void shouldMarkAsProcessedWhenResendSucceeds() {

        NotificationReprocessService service =
                new NotificationReprocessService(repository, notificationService);

        FailedNotification failure = new FailedNotification();
        failure.setId(1L);
        failure.setOrderId(10L);
        failure.setProductName("Tinta Black");
        failure.setQuantity(2);
        failure.setProcessed(false);

        when(repository.findByProcessedFalse())
                .thenReturn(List.of(failure));

        when(notificationService.notifyOrderRegistered(any(NotificationContext.class)))
                .thenReturn(true);

        service.reprocessAll();

        assertTrue(failure.getProcessed());

        verify(repository).save(failure);
    }

    @Test
    void shouldKeepUnprocessedAndNotCreateNewFailureWhenResendFailsAgain() {

        NotificationReprocessService service =
                new NotificationReprocessService(repository, notificationService);

        FailedNotification failure = new FailedNotification();
        failure.setId(1L);
        failure.setOrderId(10L);
        failure.setProductName("Tinta Black");
        failure.setQuantity(2);
        failure.setProcessed(false);

        when(repository.findByProcessedFalse())
                .thenReturn(List.of(failure));

        when(notificationService.notifyOrderRegistered(any(NotificationContext.class)))
                .thenReturn(false);

        service.reprocessAll();

        assertFalse(failure.getProcessed());

        verify(repository, never()).save(any());
    }
}
