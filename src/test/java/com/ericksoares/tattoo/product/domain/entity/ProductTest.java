package com.ericksoares.tattoo.product.domain.entity;

import com.ericksoares.tattoo.product.domain.exception.InsufficientStockException;
import com.ericksoares.tattoo.product.domain.exception.InvalidStockQuantityException;
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

    @Test
    void shouldDecreaseStock() {

        Product product = productCreatedAt(LocalDateTime.now());

        product.decreaseStock(4);

        assertEquals(6, product.getStock());
    }

    @Test
    void shouldAllowDecreasingExactlyTheRemainingStock() {

        Product product = productCreatedAt(LocalDateTime.now());

        product.decreaseStock(10);

        assertEquals(0, product.getStock());
    }

    @Test
    void shouldRejectDecreaseBeyondStockAndKeepStock() {

        Product product = productCreatedAt(LocalDateTime.now());

        assertThrows(InsufficientStockException.class, () -> product.decreaseStock(11));

        assertEquals(10, product.getStock());
    }

    @Test
    void shouldRejectZeroOrNegativeQuantityAndKeepStock() {

        Product product = productCreatedAt(LocalDateTime.now());

        assertThrows(InvalidStockQuantityException.class, () -> product.decreaseStock(0));
        assertThrows(InvalidStockQuantityException.class, () -> product.decreaseStock(-3));

        assertEquals(10, product.getStock());
    }
}
