package com.ericksoares.tattoo.report;

import java.math.BigDecimal;

record ProductSalesReportItem(
        Long productId,
        String productName,
        Integer currentStock,
        Long unitsSold,
        BigDecimal revenue,
        Long revenueRank,
        BigDecimal revenueSharePct
) {}
