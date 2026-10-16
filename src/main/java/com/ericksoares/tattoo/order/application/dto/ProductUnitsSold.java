package com.ericksoares.tattoo.order.application.dto;

import org.springframework.modulith.NamedInterface;

/** Units sold of one product across every order (see {@code OrderLookup.findUnitsSoldByProduct}). */
@NamedInterface("api")
public record ProductUnitsSold(
        Long productId,
        String productName,
        Long totalSold
) {}
