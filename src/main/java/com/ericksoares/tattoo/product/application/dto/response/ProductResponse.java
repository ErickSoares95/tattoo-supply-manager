package com.ericksoares.tattoo.product.application.dto.response;

import com.ericksoares.tattoo.product.domain.enums.ProductCategory;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        String imageUrl,
        // Nullable - the 5 products that existed before this field was added stay null
        // until backfilled via PUT /products/{id} (same one-time backfill the imageUrl
        // field needed).
        ProductCategory category,
        boolean onDeal,
        // Units sold across ALL orders, regardless of payment status - deliberately NOT
        // the same "sales" concept as report.domain.entity.ProductSalesReport (that view
        // only counts order_items with an APPROVED payment, which the storefront's
        // checkout doesn't create yet). This field powers "mais vendido" ordering for
        // the public catalog/home page; the financial report's stricter definition is
        // unchanged and stays admin-only.
        long unitsSold
) {}