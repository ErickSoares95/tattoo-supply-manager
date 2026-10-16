package com.ericksoares.tattoo.order;

import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * Test-only entry point for other modules' integration tests: the order entities, repositories
 * and RegisterOrderService are package-private, so tests outside the order module create and
 * clean up orders here and only get back ids or the public OrderSummary.
 */
public final class OrderTestFixtures {

    private OrderTestFixtures() {}

    /** Places an order through RegisterOrderService (reserves stock, publishes the event), one unit per product. */
    public static Long placeOrder(ApplicationContext context, Long userId, List<Long> productIds) {
        List<OrderItemRequest> items = productIds.stream()
                .map(productId -> new OrderItemRequest(productId, 1))
                .toList();
        return context.getBean(RegisterOrderService.class).execute(new OrderRequest(items), userId).id();
    }

    /** Persists a single-item order directly (no stock reservation, no event). */
    public static OrderSummary saveOrder(ApplicationContext context, Long userId, Long productId,
                                         String productName, int quantity, BigDecimal price) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setProductName(productName);
        item.setQuantity(quantity);
        item.setPrice(price);

        Order order = new Order();
        order.setUserId(userId);
        order.setItems(List.of(item));
        order.calculateTotal();

        Order saved = context.getBean(OrderRepository.class).save(order);
        return new OrderSummary(saved.getId(), saved.getUserId(), saved.getTotal());
    }

    public static void deleteOrders(ApplicationContext context, Collection<Long> ids) {
        context.getBean(OrderRepository.class).deleteAllById(ids);
    }
}
