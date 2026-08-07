package com.ericksoares.tattoo.ai.application.service;

import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
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

    private final ProductRepository productRepository;
    private final VectorStore vectorStore;

    public IndexProductCatalogService(ProductRepository productRepository, VectorStore vectorStore) {
        this.productRepository = productRepository;
        this.vectorStore = vectorStore;
    }

    public int indexAll() {

        List<Product> products = productRepository.findAll();

        vectorStore.add(products.stream().map(this::toDocument).toList());

        return products.size();
    }

    public void indexOne(Product product) {

        vectorStore.add(List.of(toDocument(product)));
    }

    private Document toDocument(Product product) {

        String content = "%s: %s (price: %s, stock: %d)".formatted(
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock()
        );

        return Document.builder()
                .id(deterministicId(product.getId()))
                .text(content)
                .metadata("productId", product.getId())
                .metadata("productName", product.getName())
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
