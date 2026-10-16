package com.ericksoares.tattoo.product.application.service;

import com.ericksoares.tattoo.product.application.dto.response.ReservedProduct;
import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.domain.exception.ProductNotFoundException;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public API of the product module for stock changes requested by other modules (order).
 * Runs in the caller's transaction (default propagation): the {@code Product.version}
 * optimistic lock is checked when that transaction commits, so the caller's retry on
 * OptimisticLockingFailureException (RegisterOrderService) keeps working.
 */
@NamedInterface("api")
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
     * @throws com.ericksoares.tattoo.product.domain.exception.InsufficientStockException when stock is lower than quantity
     * @throws com.ericksoares.tattoo.product.domain.exception.InvalidStockQuantityException when quantity <= 0
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
