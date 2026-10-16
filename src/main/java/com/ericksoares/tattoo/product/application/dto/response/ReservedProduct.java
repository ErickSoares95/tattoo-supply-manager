package com.ericksoares.tattoo.product.application.dto.response;

import org.springframework.modulith.NamedInterface;

import java.math.BigDecimal;

/** What {@code ProductInventory.reserve} returns: the product data an order item copies. */
@NamedInterface("api")
public record ReservedProduct(
        Long id,
        String name,
        BigDecimal unitPrice
) {}
