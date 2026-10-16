package com.ericksoares.tattoo.payment;

import com.ericksoares.tattoo.order.OrderSummary;
import com.ericksoares.tattoo.order.OrderLookup;
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
class FindOrderPaymentsService {

    private final OrderLookup orderLookup;
    private final PaymentRepository paymentRepository;

    public FindOrderPaymentsService(OrderLookup orderLookup, PaymentRepository paymentRepository) {
        this.orderLookup = orderLookup;
        this.paymentRepository = paymentRepository;
    }

    public List<PaymentResponse> execute(Long orderId, Long userId, boolean isAdmin) {

        OrderSummary order = orderLookup.findSummary(orderId);

        if (!isAdmin && !order.userId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to view payments for this order");
        }

        return paymentRepository.findByOrderIdOrderByCreationDateDesc(orderId).stream()
                .map(PaymentMapper::toResponse)
                .toList();
    }
}
