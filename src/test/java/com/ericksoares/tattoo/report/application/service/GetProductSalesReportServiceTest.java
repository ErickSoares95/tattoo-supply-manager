package com.ericksoares.tattoo.report.application.service;

import com.ericksoares.tattoo.report.application.dto.ProductSalesReportItem;
import com.ericksoares.tattoo.report.domain.entity.ProductSalesReport;
import com.ericksoares.tattoo.report.infrastructure.repository.ProductSalesReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetProductSalesReportServiceTest {

    @Mock
    private ProductSalesReportRepository repository;

    @Mock
    private ProductSalesReport row;

    @InjectMocks
    private GetProductSalesReportService service;

    @Test
    void shouldMapRepositoryRowsToItemsInRankOrder() {

        when(row.getProductId()).thenReturn(1L);
        when(row.getProductName()).thenReturn("Tinta Black");
        when(row.getCurrentStock()).thenReturn(90);
        when(row.getUnitsSold()).thenReturn(10L);
        when(row.getRevenue()).thenReturn(BigDecimal.valueOf(459.00));
        when(row.getRevenueRank()).thenReturn(1L);
        when(row.getRevenueSharePct()).thenReturn(BigDecimal.valueOf(100.00));

        when(repository.findAllByOrderByRevenueRankAsc())
                .thenReturn(List.of(row));

        List<ProductSalesReportItem> result = service.execute();

        assertEquals(1, result.size());

        ProductSalesReportItem item = result.get(0);
        assertEquals(1L, item.productId());
        assertEquals("Tinta Black", item.productName());
        assertEquals(90, item.currentStock());
        assertEquals(10L, item.unitsSold());
        assertEquals(BigDecimal.valueOf(459.00), item.revenue());
        assertEquals(1L, item.revenueRank());
        assertEquals(BigDecimal.valueOf(100.00), item.revenueSharePct());
    }

    @Test
    void shouldReturnEmptyListWhenNoSalesYet() {

        when(repository.findAllByOrderByRevenueRankAsc())
                .thenReturn(List.of());

        assertTrue(service.execute().isEmpty());
    }
}
