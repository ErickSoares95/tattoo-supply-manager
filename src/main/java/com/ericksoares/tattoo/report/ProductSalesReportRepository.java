package com.ericksoares.tattoo.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ProductSalesReportRepository extends JpaRepository<ProductSalesReport, Long> {

    List<ProductSalesReport> findAllByOrderByRevenueRankAsc();
}
