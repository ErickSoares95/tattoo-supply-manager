package com.ericksoares.tattoo.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);

    List<Payment> findByOrderIdOrderByCreationDateDesc(Long orderId);
}
