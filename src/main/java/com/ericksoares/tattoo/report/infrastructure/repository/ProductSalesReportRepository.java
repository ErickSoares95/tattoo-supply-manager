package com.ericksoares.tattoo.report.infrastructure.repository;

import com.ericksoares.tattoo.report.domain.entity.ProductSalesReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductSalesReportRepository extends JpaRepository<ProductSalesReport, Long> {

    List<ProductSalesReport> findAllByOrderByRevenueRankAsc();
}
