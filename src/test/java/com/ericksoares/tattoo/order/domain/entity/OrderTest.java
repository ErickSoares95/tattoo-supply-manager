package com.ericksoares.tattoo.order.domain.entity;

import com.ericksoares.tattoo.order.domain.exception.EmptyOrderException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void shouldCalculateTotalAsSumOfItemSubtotals() {

        OrderItem item1 = new OrderItem();
        item1.setProductId(1L);
        item1.setQuantity(2);
        item1.setPrice(BigDecimal.valueOf(10));

        OrderItem item2 = new OrderItem();
        item2.setProductId(2L);
        item2.setQuantity(3);
        item2.setPrice(BigDecimal.valueOf(5));

        Order order = new Order();
        order.setItems(List.of(item1, item2));

        order.calculateTotal();

        assertEquals(BigDecimal.valueOf(35), order.getTotal());
    }

    @Test
    void shouldThrowWhenItemsIsNull() {

        Order order = new Order();
        order.setItems(null);

        assertThrows(EmptyOrderException.class, order::validate);
    }

    @Test
    void shouldThrowWhenItemsIsEmpty() {

        Order order = new Order();
        order.setItems(Collections.emptyList());

        assertThrows(EmptyOrderException.class, order::validate);
    }

    @Test
    void shouldNotThrowWhenItemsIsNotEmpty() {

        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setQuantity(1);
        item.setPrice(BigDecimal.TEN);

        Order order = new Order();
        order.setItems(List.of(item));

        assertDoesNotThrow(order::validate);
    }
}
