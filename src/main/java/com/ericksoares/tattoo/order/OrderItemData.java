package com.ericksoares.tattoo.order;

/** Item carried by {@code OrderRegisteredEvent}; part of the order module's public API. */
public record OrderItemData(
        Long productId,
        String productName,
        Integer quantity
) {}
