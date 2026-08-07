package com.ericksoares.tattoo.notification.event.kafka;

import com.ericksoares.tattoo.notification.application.service.NotificationService;
import com.ericksoares.tattoo.notification.domain.entity.FailedPaymentNotification;
import com.ericksoares.tattoo.notification.domain.repository.FailedPaymentNotificationRepository;
import com.ericksoares.tattoo.payment.domain.entity.PaymentStatus;
import com.ericksoares.tattoo.payment.domain.event.PaymentProcessedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

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
                .thenReturn(true);

        listener.handle(event);

        verify(notificationService).notifyPaymentConfirmed(1L);
        verify(notificationService, never()).notifyPaymentRejected(any());
        verify(failedRepository, never()).save(any());
    }

    @Test
    void shouldNotifyPaymentRejectedWhenStatusIsRejected() {

        PaymentProcessedEvent event = new PaymentProcessedEvent(
                UUID.randomUUID(), 1L, PaymentStatus.REJECTED, BigDecimal.valueOf(50), LocalDateTime.now()
        );

        when(notificationService.notifyPaymentRejected(1L))
                .thenReturn(true);

        listener.handle(event);

        verify(notificationService).notifyPaymentRejected(1L);
        verify(notificationService, never()).notifyPaymentConfirmed(any());
        verify(failedRepository, never()).save(any());
    }

    @Test
    void shouldPersistFailureWhenNotificationFails() {

        PaymentProcessedEvent event = new PaymentProcessedEvent(
                UUID.randomUUID(), 1L, PaymentStatus.APPROVED, BigDecimal.valueOf(100), LocalDateTime.now()
        );

        when(notificationService.notifyPaymentConfirmed(1L))
                .thenReturn(false);

        listener.handle(event);

        verify(failedRepository).save(any(FailedPaymentNotification.class));
    }
}
