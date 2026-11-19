package com.ericksoares.tattoo.ai;

import com.ericksoares.tattoo.order.OrderCancelledEvent;
import com.ericksoares.tattoo.order.OrderItemData;
import com.ericksoares.tattoo.order.OrderRegisteredEvent;
import com.ericksoares.tattoo.product.ProductCatalog;
import com.ericksoares.tattoo.product.ProductSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A7: stock changed by orders reaches the vector index. */
@ExtendWith(MockitoExtension.class)
class ProductIndexListenerTest {

    @Mock
    private ProductCatalog productCatalog;

    @Mock
    private IndexProductCatalogService indexService;

    @InjectMocks
    private ProductIndexListener listener;

    @Test
    void reindexesEveryProductOfANewOrder() {
        ProductSnapshot ink = new ProductSnapshot(1L, "Ink", null, BigDecimal.TEN, 5);
        ProductSnapshot needles = new ProductSnapshot(2L, "Needles", null, BigDecimal.ONE, 9);
        when(productCatalog.findById(1L)).thenReturn(Optional.of(ink));
        when(productCatalog.findById(2L)).thenReturn(Optional.of(needles));

        listener.handle(new OrderRegisteredEvent(10L,
                List.of(new OrderItemData(1L, "Ink", 2), new OrderItemData(2L, "Needles", 1)), LocalDateTime.now()));

        verify(indexService).indexOne(ink);
        verify(indexService).indexOne(needles);
    }

    @Test
    void reindexesTheProductsOfACancelledOrder() {
        ProductSnapshot ink = new ProductSnapshot(1L, "Ink", null, BigDecimal.TEN, 5);
        when(productCatalog.findById(1L)).thenReturn(Optional.of(ink));

        listener.handle(new OrderCancelledEvent(10L, List.of(new OrderItemData(1L, "Ink", 2)), true, LocalDateTime.now()));

        verify(indexService).indexOne(ink);
    }
}
