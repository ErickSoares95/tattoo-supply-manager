package com.ericksoares.tattoo.order.application.mapper;

import com.ericksoares.tattoo.order.application.dto.OrderItemResponse;
import com.ericksoares.tattoo.order.application.dto.OrderResponse;
import com.ericksoares.tattoo.order.domain.entity.Order;
import com.ericksoares.tattoo.order.domain.entity.OrderItem;

public class OrderMapper {

    private OrderMapper() {}

    public static OrderItemResponse toResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPrice(),
                item.getSubtotal()
        );
    }

    public static OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getItems().stream()
                        .map(OrderMapper::toResponse)
                        .toList(),
                order.getTotal(),
                order.getCreationDate()
        );
    }
}
