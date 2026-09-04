package com.ericksoares.tattoo.product.domain.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    private Product productCreatedAt(LocalDateTime creationDate) {
        Product product = Product.builder()
                .name("Tattoo Ink - Black")
                .price(BigDecimal.valueOf(45.90))
                .stock(10)
                .build();
        product.setCreationDate(creationDate);
        return product;
    }

    @Test
    void shouldBeOnDailyDealWhenCreatedMoreThanThreeMonthsAgo() {

        Product product = productCreatedAt(LocalDateTime.now().minusMonths(4));

        assertTrue(product.isOnDailyDeal());
    }

    @Test
    void shouldNotBeOnDailyDealWhenCreatedLessThanThreeMonthsAgo() {

        Product product = productCreatedAt(LocalDateTime.now().minusMonths(1));

        assertFalse(product.isOnDailyDeal());
    }

    @Test
    void shouldNotBeOnDailyDealWhenCreationDateIsNull() {

        Product product = productCreatedAt(null);

        assertFalse(product.isOnDailyDeal());
    }
}
