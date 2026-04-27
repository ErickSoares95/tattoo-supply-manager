package com.ericksoares.tattoo.order.application.service;

import com.ericksoares.tattoo.order.application.dto.OrderRequest;
import com.ericksoares.tattoo.order.domain.entity.Order;
import com.ericksoares.tattoo.order.domain.entity.OrderItem;
import com.ericksoares.tattoo.order.domain.event.OrderRegisteredEvent;
import com.ericksoares.tattoo.order.infrasctruture.repository.OrderRepository;
import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.domain.exception.ProductNotFoundException;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
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
    public Order execute(OrderRequest request) {

        List<OrderItem> items = request.items().stream().map(reqItem -> {

            Product product = productRepository.findById(reqItem.productId())
                    .orElseThrow(() -> new ProductNotFoundException(reqItem.productId()));

            product.decreaseStock(reqItem.quantity());

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setQuantity(reqItem.quantity());
            item.setPrice(product.getPrice());

            item.validate();

            return item;

        }).toList();

        Order order = new Order();
        order.setItems(items);

        order.validate();
        order.calculateTotal();

        Order savedOrder = orderRepository.save(order);

        publisher.publishEvent(
                new OrderRegisteredEvent(
                    savedOrder.getId(),
                    LocalDateTime.now()
                )
        );
        return savedOrder;
    }
}