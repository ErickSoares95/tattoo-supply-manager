package com.ericksoares.tattoo.notification.domain.repository;

import com.ericksoares.tattoo.notification.domain.entity.FailedPaymentNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FailedPaymentNotificationRepository
        extends JpaRepository<FailedPaymentNotification, Long> {

    List<FailedPaymentNotification> findByProcessedFalse();
}
