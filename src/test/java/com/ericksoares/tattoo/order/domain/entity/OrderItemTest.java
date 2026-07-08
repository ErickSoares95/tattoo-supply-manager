package com.ericksoares.tattoo.order.domain.entity;

import com.ericksoares.tattoo.order.domain.exception.InvalidOrderItemQuantityException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {

    @Test
    void shouldCalculateSubtotalAsPriceTimesQuantity() {

        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setQuantity(4);
        item.setPrice(BigDecimal.valueOf(12.5));

        assertEquals(BigDecimal.valueOf(50.0), item.getSubtotal());
    }

    @Test
    void shouldThrowWhenQuantityIsZero() {

        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setQuantity(0);
        item.setPrice(BigDecimal.TEN);

        assertThrows(InvalidOrderItemQuantityException.class, item::validate);
    }

    @Test
    void shouldThrowWhenQuantityIsNegative() {

        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setQuantity(-1);
        item.setPrice(BigDecimal.TEN);

        assertThrows(InvalidOrderItemQuantityException.class, item::validate);
    }

    @Test
    void shouldNotThrowWhenQuantityIsPositive() {

        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setQuantity(1);
        item.setPrice(BigDecimal.TEN);

        assertDoesNotThrow(item::validate);
    }
}
