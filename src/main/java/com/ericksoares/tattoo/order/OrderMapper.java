package com.ericksoares.tattoo.order;

class OrderMapper {

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
