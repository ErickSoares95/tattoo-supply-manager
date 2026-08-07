package com.ericksoares.tattoo.report.application.dto;

import java.math.BigDecimal;

public record ProductSalesReportItem(
        Long productId,
        String productName,
        Integer currentStock,
        Long unitsSold,
        BigDecimal revenue,
        Long revenueRank,
        BigDecimal revenueSharePct
) {}
