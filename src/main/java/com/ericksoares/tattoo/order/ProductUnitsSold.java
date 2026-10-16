package com.ericksoares.tattoo.order;

/** Units sold of one product across every order (see {@code OrderLookup.findUnitsSoldByProduct}). */
public record ProductUnitsSold(
        Long productId,
        String productName,
        Long totalSold
) {}
