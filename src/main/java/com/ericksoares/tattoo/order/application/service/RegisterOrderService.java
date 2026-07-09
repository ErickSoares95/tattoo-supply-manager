package com.ericksoares.tattoo.order.application.service;

import com.ericksoares.tattoo.order.application.dto.OrderItemData;
import com.ericksoares.tattoo.order.application.dto.OrderRequest;
import com.ericksoares.tattoo.order.application.dto.OrderResponse;
import com.ericksoares.tattoo.order.application.mapper.OrderMapper;
import com.ericksoares.tattoo.order.domain.entity.Order;
import com.ericksoares.tattoo.order.domain.entity.OrderItem;
import com.ericksoares.tattoo.order.domain.event.OrderRegisteredEvent;
import com.ericksoares.tattoo.order.infrasctruture.repository.OrderRepository;
import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.domain.exception.ProductNotFoundException;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RegisterOrderService {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher publisher;
    private final OrderRepository orderRepository;

    public RegisterOrderService(
                                ProductRepository productRepository,
                                OrderRepository orderRepository,
                                ApplicationEventPublisher publisher
                                )
    {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.publisher = publisher;
    }

    @Transactional
    public OrderResponse execute(OrderRequest request, Long userId) {

        List<OrderItem> items = new ArrayList<>();
        List<OrderItemData> eventItems = new ArrayList<>();

        for (var reqItem : request.items()) {

            Product product = productRepository.findById(reqItem.productId())
                    .orElseThrow(() -> new ProductNotFoundException(reqItem.productId()));

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setQuantity(reqItem.quantity());
            item.setPrice(product.getPrice());

            item.validate();

            product.decreaseStock(reqItem.quantity());

            items.add(item);

            eventItems.add(new OrderItemData(
                    product.getId(),
                    product.getName(),
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