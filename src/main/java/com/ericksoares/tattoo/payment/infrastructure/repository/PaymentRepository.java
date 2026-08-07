package com.ericksoares.tattoo.payment.infrastructure.repository;

import com.ericksoares.tattoo.payment.domain.entity.Payment;
import com.ericksoares.tattoo.payment.domain.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
