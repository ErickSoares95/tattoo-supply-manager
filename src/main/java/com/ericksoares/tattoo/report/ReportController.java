package com.ericksoares.tattoo.report;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reports")
class ReportController {

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
