package com.ericksoares.tattoo.ai.application.service;

import com.ericksoares.tattoo.product.application.dto.response.ProductSnapshot;
import com.ericksoares.tattoo.product.application.service.ProductCatalog;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Indexes the product catalog into the vector store, one Document per
 * product. Re-indexing the same product id is an upsert (PgVectorStore does
 * ON CONFLICT (id) DO UPDATE), so this is safe to call repeatedly.
 */
@Service
public class IndexProductCatalogService {

    private final ProductCatalog productCatalog;
    private final VectorStore vectorStore;

    public IndexProductCatalogService(ProductCatalog productCatalog, VectorStore vectorStore) {
        this.productCatalog = productCatalog;
        this.vectorStore = vectorStore;
    }

    public int indexAll() {

        List<ProductSnapshot> products = productCatalog.findAll();

        vectorStore.add(products.stream().map(this::toDocument).toList());

        return products.size();
    }

    public void indexOne(ProductSnapshot product) {

        vectorStore.add(List.of(toDocument(product)));
    }

    private Document toDocument(ProductSnapshot product) {

        String content = "%s: %s (price: %s, stock: %d)".formatted(
                product.name(),
                product.description(),
                product.price(),
                product.stock()
        );

        return Document.builder()
                .id(deterministicId(product.id()))
                .text(content)
                .metadata("productId", product.id())
                .metadata("productName", product.name())
                .build();
    }

    /**
     * PgVectorStore's id column is a UUID, so a plain "7" is rejected. This
     * derives a stable UUID from the product id (same input -> same UUID
     * every time), which is what keeps re-indexing idempotent (upsert on
     * conflict) instead of piling up duplicate rows per product.
     */
    private String deterministicId(Long productId) {

        return UUID.nameUUIDFromBytes(
                ("product-" + productId).getBytes(StandardCharsets.UTF_8)
        ).toString();
    }
}
