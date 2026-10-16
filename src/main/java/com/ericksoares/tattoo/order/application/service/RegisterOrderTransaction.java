package com.ericksoares.tattoo.order.application.service;

import com.ericksoares.tattoo.order.application.dto.OrderItemData;
import com.ericksoares.tattoo.order.application.dto.OrderRequest;
import com.ericksoares.tattoo.order.application.dto.OrderResponse;
import com.ericksoares.tattoo.order.application.mapper.OrderMapper;
import com.ericksoares.tattoo.order.domain.entity.Order;
import com.ericksoares.tattoo.order.domain.entity.OrderItem;
import com.ericksoares.tattoo.order.domain.event.OrderRegisteredEvent;
import com.ericksoares.tattoo.order.infrasctruture.repository.OrderRepository;
import com.ericksoares.tattoo.product.application.dto.response.ReservedProduct;
import com.ericksoares.tattoo.product.application.service.ProductInventory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One attempt at registering an order, in its own transaction. Kept in a separate bean from
 * {@link RegisterOrderService} so the retry on optimistic-lock failures wraps the transaction
 * (each attempt reopens a fresh one and re-reads the products) instead of running inside it.
 * Stock goes through the product module's public {@link ProductInventory}, which joins this
 * transaction, so the optimistic lock still fails at this transaction's commit.
 */
@Service
public class RegisterOrderTransaction {

    private final ProductInventory productInventory;
    private final ApplicationEventPublisher publisher;
    private final OrderRepository orderRepository;

    public RegisterOrderTransaction(
                                ProductInventory productInventory,
                                OrderRepository orderRepository,
                                ApplicationEventPublisher publisher
                                )
    {
        this.productInventory = productInventory;
        this.orderRepository = orderRepository;
        this.publisher = publisher;
    }

    @Transactional
    public OrderResponse execute(OrderRequest request, Long userId) {

        List<OrderItem> items = new ArrayList<>();
        List<OrderItemData> eventItems = new ArrayList<>();

        for (var reqItem : request.items()) {

            OrderItem item = new OrderItem();
            item.setQuantity(reqItem.quantity());

            // Before touching stock: a non-positive quantity is an order error, not a stock one.
            item.validate();

            ReservedProduct product = productInventory.reserve(reqItem.productId(), reqItem.quantity());

            item.setProductId(product.id());
            item.setProductName(product.name());
            item.setPrice(product.unitPrice());

            items.add(item);

            eventItems.add(new OrderItemData(
                    product.id(),
                    product.name(),
                    reqItem.quantity()
            ));
        }

        Order order = new Order();
        order.setItems(items);
        order.setUserId(userId);

        order.validate();
        order.calculateTotal();

        Order savedOrder = orderRepository.save(order);

        publisher.publishEvent(
                new OrderRegisteredEvent(
                        savedOrder.getId(),
                        eventItems,
                        LocalDateTime.now()
                )
        );

        return OrderMapper.toResponse(savedOrder);
    }
}
