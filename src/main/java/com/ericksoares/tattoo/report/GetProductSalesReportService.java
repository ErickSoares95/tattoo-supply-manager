package com.ericksoares.tattoo.report;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
class GetProductSalesReportService {

    private final ProductSalesReportRepository repository;

    public GetProductSalesReportService(ProductSalesReportRepository repository) {
        this.repository = repository;
    }

    public List<ProductSalesReportItem> execute() {
        return repository.findAllByOrderByRevenueRankAsc()
                .stream()
                .map(ProductSalesReportMapper::toItem)
                .toList();
    }
}
