package com.ericksoares.tattoo.report;

class ProductSalesReportMapper {

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
