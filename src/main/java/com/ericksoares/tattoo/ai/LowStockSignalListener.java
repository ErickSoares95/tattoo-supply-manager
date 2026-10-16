package com.ericksoares.tattoo.ai;

import com.ericksoares.tattoo.order.OrderItemData;
import com.ericksoares.tattoo.order.OrderRegisteredEvent;
import com.ericksoares.tattoo.product.ProductSnapshot;
import com.ericksoares.tattoo.product.ProductCatalog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacts to every OrderRegisteredEvent to signal ordered products whose stock is at or
 * below the low-stock threshold. It fires on every order while the stock stays low, not
 * only when it first crosses the threshold. The order/product modules have no idea this
 * listener exists — same decoupling already used by
 * {@code notification.NotificationListener}.
 */
@Slf4j
@Component
class LowStockSignalListener {

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
