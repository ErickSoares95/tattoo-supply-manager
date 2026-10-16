package com.ericksoares.tattoo.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public API of the product module for stock changes requested by other modules (order).
 * Runs in the caller's transaction (default propagation): the {@code Product.version}
 * optimistic lock is checked when that transaction commits, so the caller's retry on
 * OptimisticLockingFailureException (RegisterOrderService) keeps working.
 */
@Service
public class ProductInventory {

    private final ProductRepository productRepository;

    public ProductInventory(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Decreases the stock of a product by {@code quantity}.
     *
     * @throws ProductNotFoundException when the product doesn't exist
     * @throws com.ericksoares.tattoo.product.InsufficientStockException when stock is lower than quantity
     * @throws com.ericksoares.tattoo.product.InvalidStockQuantityException when quantity <= 0
     */
    @Transactional
    public ReservedProduct reserve(Long productId, int quantity) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        product.decreaseStock(quantity);

        productRepository.save(product);

        return new ReservedProduct(product.getId(), product.getName(), product.getPrice());
    }
}
