package com.ericksoares.tattoo.report.application.mapper;

import com.ericksoares.tattoo.report.application.dto.ProductSalesReportItem;
import com.ericksoares.tattoo.report.domain.entity.ProductSalesReport;

public class ProductSalesReportMapper {

    private ProductSalesReportMapper() {}

    public static ProductSalesReportItem toItem(ProductSalesReport row) {
        return new ProductSalesReportItem(
                row.getProductId(),
                row.getProductName(),
                row.getCurrentStock(),
                row.getUnitsSold(),
                row.getRevenue(),
                row.getRevenueRank(),
                row.getRevenueSharePct()
        );
    }
}
