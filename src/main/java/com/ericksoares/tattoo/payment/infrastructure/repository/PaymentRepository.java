package com.ericksoares.tattoo.payment.infrastructure.repository;

import com.ericksoares.tattoo.payment.domain.entity.Payment;
import com.ericksoares.tattoo.payment.domain.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);

    List<Payment> findByOrderIdOrderByCreationDateDesc(Long orderId);
}
