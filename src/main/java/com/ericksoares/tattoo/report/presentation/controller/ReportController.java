package com.ericksoares.tattoo.report.presentation.controller;

import com.ericksoares.tattoo.report.application.dto.ProductSalesReportItem;
import com.ericksoares.tattoo.report.application.service.GetProductSalesReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reports")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ReportController {

    private final GetProductSalesReportService service;

    public ReportController(GetProductSalesReportService service) {
        this.service = service;
    }

    @GetMapping("/product-sales")
    @PreAuthorize("hasAnyRole('ADMIN','ATTENDANT')")
    public ResponseEntity<List<ProductSalesReportItem>> productSales() {
        return ResponseEntity.ok(service.execute());
    }
}
