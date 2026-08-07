package com.ericksoares.tattoo.ai.event;

import com.ericksoares.tattoo.ai.application.service.IndexProductCatalogService;
import com.ericksoares.tattoo.product.domain.event.ProductRegisteredEvent;
import com.ericksoares.tattoo.product.domain.event.ProductUpdatedEvent;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
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
public class ProductIndexListener {

    private final ProductRepository productRepository;
    private final IndexProductCatalogService indexProductCatalogService;

    public ProductIndexListener(
            ProductRepository productRepository,
            IndexProductCatalogService indexProductCatalogService
    ) {
        this.productRepository = productRepository;
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

    private void reindex(Long productId) {

        productRepository.findById(productId).ifPresent(product -> {

            indexProductCatalogService.indexOne(product);

            log.info("Reindexed product {} in the vector store", productId);
        });
    }
}
