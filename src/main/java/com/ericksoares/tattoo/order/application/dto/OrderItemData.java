package com.ericksoares.tattoo.order.application.dto;

import org.springframework.modulith.NamedInterface;

/** Item carried by {@code OrderRegisteredEvent}; part of the order module's public API. */
@NamedInterface("api")
public record OrderItemData(
        Long productId,
        String productName,
        Integer quantity
) {}
