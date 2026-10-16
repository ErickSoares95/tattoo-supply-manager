package com.ericksoares.tattoo.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Public read API of the product module (used by ai). Never exposes the Product entity. */
@Service
@Transactional(readOnly = true)
public class ProductCatalog {

    private final ProductRepository productRepository;

    public ProductCatalog(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductSnapshot> findAll() {
        return productRepository.findAll().stream().map(ProductCatalog::toSnapshot).toList();
    }

    public List<ProductSnapshot> findAllById(Collection<Long> ids) {
        return productRepository.findAllById(ids).stream().map(ProductCatalog::toSnapshot).toList();
    }

    public Optional<ProductSnapshot> findById(Long id) {
        return productRepository.findById(id).map(ProductCatalog::toSnapshot);
    }

    private static ProductSnapshot toSnapshot(Product product) {
        return new ProductSnapshot(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock()
        );
    }
}
