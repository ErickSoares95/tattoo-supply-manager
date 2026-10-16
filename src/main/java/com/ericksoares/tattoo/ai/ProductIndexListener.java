package com.ericksoares.tattoo.ai;

import com.ericksoares.tattoo.product.ProductDeletedEvent;
import com.ericksoares.tattoo.product.ProductRegisteredEvent;
import com.ericksoares.tattoo.product.ProductUpdatedEvent;
import com.ericksoares.tattoo.product.ProductCatalog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Keeps the vector store in sync with the product catalog reactively.
 * ProductRegisteredEvent already existed but had no listener anywhere in the
 * codebase before this — it is finally consumed here, alongside the new
 * ProductUpdatedEvent. Neither the product module nor the order module knows
 * this listener (or the ai module) exists.
 */
@Slf4j
@Component
class ProductIndexListener {

    private final ProductCatalog productCatalog;
    private final IndexProductCatalogService indexProductCatalogService;

    public ProductIndexListener(
            ProductCatalog productCatalog,
            IndexProductCatalogService indexProductCatalogService
    ) {
        this.productCatalog = productCatalog;
        this.indexProductCatalogService = indexProductCatalogService;
    }

    @Async("aiExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ProductRegisteredEvent event) {
        reindex(event.productId());
    }

    @Async("aiExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ProductUpdatedEvent event) {
        reindex(event.productId());
    }

    @Async("aiExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ProductDeletedEvent event) {
        indexProductCatalogService.removeOne(event.productId());
        log.info("Removed product {} from the vector store", event.productId());
    }

    private void reindex(Long productId) {

        productCatalog.findById(productId).ifPresent(product -> {

            indexProductCatalogService.indexOne(product);

            log.info("Reindexed product {} in the vector store", productId);
        });
    }
}
