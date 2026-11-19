package com.ericksoares.tattoo.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public command API of the order module for the payment module. Called directly (not via an
 * event) because the order must become PAID in the same transaction as the approved payment:
 * if a cancellation committed first, Order.version makes this transaction fail and the
 * payment rolls back with it.
 */
@Service
public class OrderPayments {

    private final OrderRepository orderRepository;

    public OrderPayments(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * @throws OrderNotFoundException when the order doesn't exist
     * @throws OrderNotPayableException when the order was cancelled
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void markPaid(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.markPaid();
        // Flush now so a concurrent cancellation surfaces here, inside the payment's transaction.
        orderRepository.saveAndFlush(order);
    }
}
