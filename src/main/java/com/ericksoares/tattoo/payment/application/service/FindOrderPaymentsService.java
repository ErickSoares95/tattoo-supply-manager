package com.ericksoares.tattoo.payment.application.service;

import com.ericksoares.tattoo.order.domain.entity.Order;
import com.ericksoares.tattoo.order.domain.exception.OrderNotFoundException;
import com.ericksoares.tattoo.order.infrasctruture.repository.OrderRepository;
import com.ericksoares.tattoo.payment.application.dto.PaymentResponse;
import com.ericksoares.tattoo.payment.application.mapper.PaymentMapper;
import com.ericksoares.tattoo.payment.infrastructure.repository.PaymentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read side of the payment module: lists every payment attempt for an order, newest
 * first, so the storefront can show whether an order is paid. Same owner-or-admin rule
 * as {@code ProcessPaymentService} / {@code FindOrderByIdService}.
 */
@Service
@Transactional(readOnly = true)
public class FindOrderPaymentsService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public FindOrderPaymentsService(OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    public List<PaymentResponse> execute(Long orderId, Long userId, boolean isAdmin) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!isAdmin && !order.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to view payments for this order");
        }

        return paymentRepository.findByOrderIdOrderByCreationDateDesc(orderId).stream()
                .map(PaymentMapper::toResponse)
                .toList();
    }
}
