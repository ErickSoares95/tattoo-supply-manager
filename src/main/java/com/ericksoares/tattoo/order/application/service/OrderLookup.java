package com.ericksoares.tattoo.order.application.service;

import com.ericksoares.tattoo.order.application.dto.OrderSummary;
import com.ericksoares.tattoo.order.application.dto.ProductUnitsSold;
import com.ericksoares.tattoo.order.domain.entity.Order;
import com.ericksoares.tattoo.order.domain.exception.OrderNotFoundException;
import com.ericksoares.tattoo.order.infrasctruture.repository.OrderItemRepository;
import com.ericksoares.tattoo.order.infrasctruture.repository.OrderRepository;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Public read API of the order module (used by payment and ai). Never exposes the JPA entities. */
@NamedInterface("api")
@Service
@Transactional(readOnly = true)
public class OrderLookup {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderLookup(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    /** @throws OrderNotFoundException when the order doesn't exist */
    public OrderSummary findSummary(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return new OrderSummary(order.getId(), order.getUserId(), order.getTotal());
    }

    /** Units sold per product over all orders, best sellers first. */
    public List<ProductUnitsSold> findUnitsSoldByProduct() {

        return orderItemRepository.findProductSalesSummary().stream()
                .map(row -> new ProductUnitsSold(row.getProductId(), row.getProductName(), row.getTotalSold()))
                .toList();
    }
}
