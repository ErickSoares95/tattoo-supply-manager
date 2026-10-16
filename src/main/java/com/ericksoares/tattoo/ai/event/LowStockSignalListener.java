package com.ericksoares.tattoo.ai.event;

import com.ericksoares.tattoo.order.application.dto.OrderItemData;
import com.ericksoares.tattoo.order.domain.event.OrderRegisteredEvent;
import com.ericksoares.tattoo.product.application.dto.response.ProductSnapshot;
import com.ericksoares.tattoo.product.application.service.ProductCatalog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacts to every OrderRegisteredEvent to signal products that just crossed
 * below the low-stock threshold. The order/product modules have no idea this
 * listener exists — same decoupling already used by
 * {@code notification.event.NotificationListener}.
 */
@Slf4j
@Component
public class LowStockSignalListener {

    private static final int LOW_STOCK_THRESHOLD = 10;

    private final ProductCatalog productCatalog;

    public LowStockSignalListener(ProductCatalog productCatalog) {
        this.productCatalog = productCatalog;
    }

    @Async("aiExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderRegisteredEvent event) {

        for (OrderItemData item : event.items()) {

            productCatalog.findById(item.productId())
                    .filter(product -> product.stock() <= LOW_STOCK_THRESHOLD)
                    .ifPresent(this::signalLowStock);
        }
    }

    private void signalLowStock(ProductSnapshot product) {

        log.warn(
                "LOW STOCK signal: product '{}' (id {}) has only {} units left (threshold {})",
                product.name(),
                product.id(),
                product.stock(),
                LOW_STOCK_THRESHOLD
        );
    }
}
