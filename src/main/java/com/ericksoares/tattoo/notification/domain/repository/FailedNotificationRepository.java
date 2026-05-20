package com.ericksoares.tattoo.notification.domain.repository;

import com.ericksoares.tattoo.notification.domain.entity.FailedNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FailedNotificationRepository
        extends JpaRepository<FailedNotification, Long> {

    List<FailedNotification> findByProcessedFalse();
}
