package com.ericksoares.tattoo.product;

import java.math.BigDecimal;

record ProductResponse(
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
        // the same "sales" concept as report.ProductSalesReport (that view
        // only counts order_items with an APPROVED payment; orders left unpaid or with a
        // rejected payment still count here). This field powers "mais vendido" ordering for
        // the public catalog/home page; the financial report's stricter definition is
        // unchanged and stays admin-only.
        long unitsSold,
        // Optimistic-lock version; the admin form sends it back on PUT so an edit made on a
        // stale copy is refused (409) instead of overwriting stock sold in the meantime.
        Long version
) {}