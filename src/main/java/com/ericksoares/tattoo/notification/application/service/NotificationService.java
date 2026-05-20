package com.ericksoares.tattoo.notification.application.service;

import com.ericksoares.tattoo.notification.application.dto.NotificationContext;
import com.ericksoares.tattoo.notification.application.listener.NotificationSender;
import com.ericksoares.tattoo.notification.domain.entity.FailedNotification;
import com.ericksoares.tattoo.notification.domain.repository.FailedNotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class NotificationService {

    private final List<NotificationSender> senders;
    private final FailedNotificationRepository failedRepository;

    public NotificationService(List<NotificationSender> senders, FailedNotificationRepository failedRepository) {
        this.senders = senders;
        this.failedRepository = failedRepository;
    }

    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void notifyOrderRegistered(NotificationContext context) {
        log.info(
                "Sending notification for order {}",
                context.orderId()
        );
        for (NotificationSender sender : senders) {
            sender.sendOrderRegistered(context.orderId());
        }
    }

    @Recover
    public void recover(Exception e, NotificationContext context) {
        log.error(
                "FINAL FAILURE - orderId: {}, product: {}, quantity: {}",
                context.orderId(),
                context.productName(),
                context.quantity(),
                e
        );
        FailedNotification failure = new FailedNotification();

        failure.setOrderId(context.orderId());
        failure.setProductName(context.productName());
        failure.setQuantity(context.quantity());
        failure.setErrorMessage(e.getMessage());
        failure.setCreatedAt(LocalDateTime.now());
        failure.setProcessed(false);

        failedRepository.save(failure);

        log.warn(
                "Failed notification persisted for order {}",
                context.orderId()
        );
    }
}
