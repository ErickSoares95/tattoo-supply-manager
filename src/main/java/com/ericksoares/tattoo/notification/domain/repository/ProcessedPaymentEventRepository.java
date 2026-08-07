package com.ericksoares.tattoo.notification.domain.repository;

import com.ericksoares.tattoo.notification.domain.entity.ProcessedPaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedPaymentEventRepository extends JpaRepository<ProcessedPaymentEvent, Long> {
}
