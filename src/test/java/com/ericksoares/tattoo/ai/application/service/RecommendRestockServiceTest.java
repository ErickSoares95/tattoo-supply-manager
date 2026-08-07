package com.ericksoares.tattoo.ai.application.service;

import com.ericksoares.tattoo.ai.application.dto.RestockRecommendationResponse;
import com.ericksoares.tattoo.order.infrasctruture.repository.OrderItemRepository;
import com.ericksoares.tattoo.order.infrasctruture.repository.OrderItemRepository.ProductSalesProjection;
import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendRestockServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private LlmClient llmClient;

    @InjectMocks
    private RecommendRestockService service;

    @Test
    void shouldReturnCannedResponseWithoutCallingLlmWhenNoSalesHistory() {

        when(orderItemRepository.findProductSalesSummary())
                .thenReturn(List.of());

        RestockRecommendationResponse response = service.execute();

        assertTrue(response.basedOn().isEmpty());
        verifyNoInteractions(llmClient);
    }

    @Test
    void shouldBuildContextAndReturnLlmRecommendation() {

        ProductSalesProjection row = mock(ProductSalesProjection.class);
        when(row.getProductId()).thenReturn(1L);
        when(row.getProductName()).thenReturn("Tattoo Ink - Black");
        when(row.getTotalSold()).thenReturn(50L);

        when(orderItemRepository.findProductSalesSummary())
                .thenReturn(List.of(row));

        Product product = Product.builder()
                .name("Tattoo Ink - Black")
                .stock(5)
                .build();
        product.setId(1L);

        when(productRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(product));

        when(llmClient.complete(anyString()))
                .thenReturn("Restock Tattoo Ink - Black soon.");

        RestockRecommendationResponse response = service.execute();

        assertEquals("Restock Tattoo Ink - Black soon.", response.recommendation());
        assertEquals(1, response.basedOn().size());
        assertEquals(5, response.basedOn().get(0).currentStock());
        assertEquals(50L, response.basedOn().get(0).totalSold());
    }
}
