package com.ericksoares.tattoo.order;

import com.ericksoares.tattoo.product.ProductInventory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Cancels an unpaid order and gives its stock back (O4). Only PENDING_PAYMENT orders can be
 * cancelled; Order.version makes a cancellation racing with an approved payment fail (409)
 * instead of producing an order that is both paid and cancelled.
 */
@Service
@RequiredArgsConstructor
class CancelOrderService {

    private final OrderRepository repository;
    private final ProductInventory productInventory;
    private final ApplicationEventPublisher publisher;

    /** POST /orders/{id}/cancel - the owner or an admin. */
    @Transactional
    public OrderResponse execute(Long orderId, Long userId, boolean isAdmin) {
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!isAdmin && !order.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to cancel this order");
        }

        return OrderMapper.toResponse(cancel(order, false));
    }

    /** Used by OrderExpirationJob, one order per transaction. */
    @Transactional
    public void cancelExpired(Long orderId) {
        repository.findById(orderId)
                .filter(order -> order.getStatus() == OrderStatus.PENDING_PAYMENT)
                .ifPresent(order -> cancel(order, true));
    }

    private Order cancel(Order order, boolean expired) {
        order.cancel();

        order.getItems().forEach(item -> productInventory.release(item.getProductId(), item.getQuantity()));

        Order saved = repository.saveAndFlush(order);

        publisher.publishEvent(new OrderCancelledEvent(
                saved.getId(),
                saved.getItems().stream()
                        .map(item -> new OrderItemData(item.getProductId(), item.getProductName(), item.getQuantity()))
                        .toList(),
                expired,
                LocalDateTime.now()
        ));
        return saved;
    }
}
