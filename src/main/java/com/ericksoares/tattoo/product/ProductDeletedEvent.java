package com.ericksoares.tattoo.product;

/** Published after a product is deleted, so listeners (the ai vector index) can drop it. */
public record ProductDeletedEvent(
        Long productId
) {}
