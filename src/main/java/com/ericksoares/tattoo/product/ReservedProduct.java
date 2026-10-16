package com.ericksoares.tattoo.product;

import java.math.BigDecimal;

/** What {@code ProductInventory.reserve} returns: the product data an order item copies. */
public record ReservedProduct(
        Long id,
        String name,
        BigDecimal unitPrice
) {}
