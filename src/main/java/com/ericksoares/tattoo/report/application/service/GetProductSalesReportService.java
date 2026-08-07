package com.ericksoares.tattoo.report.application.service;

import com.ericksoares.tattoo.report.application.dto.ProductSalesReportItem;
import com.ericksoares.tattoo.report.application.mapper.ProductSalesReportMapper;
import com.ericksoares.tattoo.report.infrastructure.repository.ProductSalesReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GetProductSalesReportService {

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
