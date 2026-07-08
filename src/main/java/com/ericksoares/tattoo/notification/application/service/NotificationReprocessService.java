package com.ericksoares.tattoo.notification.application.service;

import com.ericksoares.tattoo.notification.application.dto.NotificationContext;
import com.ericksoares.tattoo.notification.domain.entity.FailedNotification;
import com.ericksoares.tattoo.notification.domain.repository.FailedNotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class NotificationReprocessService {

    private final FailedNotificationRepository repository;
    private final NotificationService notificationService;

    public NotificationReprocessService(FailedNotificationRepository repository, NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    public void reprocessAll() {

        List<FailedNotification> failures = repository.findByProcessedFalse();

        for (FailedNotification failure : failures) {

            try {

                NotificationContext context = new NotificationContext(
                        failure.getOrderId(),
                        failure.getProductName(),
                        failure.getQuantity()
                );

                boolean succeeded = notificationService.notifyOrderRegistered(context);

                if (succeeded) {
                    failure.setProcessed(true);
                    repository.save(failure);
                } else {
                    log.warn("Reprocess failed again for id {}", failure.getId());
                }

            } catch (Exception e) {
                log.error("Unexpected error reprocessing id {}", failure.getId(), e);
            }
        }
    }
}
