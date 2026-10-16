package com.ericksoares.tattoo.ai;

import com.ericksoares.tattoo.order.ProductUnitsSold;
import com.ericksoares.tattoo.order.OrderLookup;
import com.ericksoares.tattoo.product.ProductSnapshot;
import com.ericksoares.tattoo.product.ProductCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendRestockServiceTest {

    @Mock
    private OrderLookup orderLookup;

    @Mock
    private ProductCatalog productCatalog;

    @Mock
    private LlmClient llmClient;

    @InjectMocks
    private RecommendRestockService service;

    @Test
    void shouldReturnCannedResponseWithoutCallingLlmWhenNoSalesHistory() {

        when(orderLookup.findUnitsSoldByProduct())
                .thenReturn(List.of());

        RestockRecommendationResponse response = service.execute();

        assertTrue(response.basedOn().isEmpty());
        verifyNoInteractions(llmClient);
    }

    @Test
    void shouldBuildContextAndReturnLlmRecommendation() {

        when(orderLookup.findUnitsSoldByProduct())
                .thenReturn(List.of(new ProductUnitsSold(1L, "Tattoo Ink - Black", 50L)));

        when(productCatalog.findAllById(List.of(1L)))
                .thenReturn(List.of(new ProductSnapshot(1L, "Tattoo Ink - Black", null, null, 5)));

        when(llmClient.complete(anyString()))
                .thenReturn("Restock Tattoo Ink - Black soon.");

        RestockRecommendationResponse response = service.execute();

        assertEquals("Restock Tattoo Ink - Black soon.", response.recommendation());
        assertEquals(1, response.basedOn().size());
        assertEquals(5, response.basedOn().get(0).currentStock());
        assertEquals(50L, response.basedOn().get(0).totalSold());
    }
}
