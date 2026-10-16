package com.ericksoares.tattoo.product;

import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Test-only entry point for other modules' integration tests: Product and ProductRepository
 * are package-private, so tests outside the product module seed and clean up products here
 * and read them back through the public ProductCatalog API.
 */
public final class ProductTestFixtures {

    private ProductTestFixtures() {}

    public static ProductSnapshot createProduct(ApplicationContext context, String name, BigDecimal price, int stock) {
        Product product = context.getBean(ProductRepository.class).save(
                Product.builder().name(name).price(price).stock(stock).build()
        );
        return new ProductSnapshot(product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), product.getStock());
    }

    public static void deleteProducts(ApplicationContext context, Collection<Long> ids) {
        context.getBean(ProductRepository.class).deleteAllById(ids);
    }
}
