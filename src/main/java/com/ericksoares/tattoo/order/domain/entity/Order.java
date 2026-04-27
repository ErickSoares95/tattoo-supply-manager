package com.ericksoares.tattoo.order.domain.entity;

import com.ericksoares.tattoo.order.domain.exception.EmptyOrderException;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items;

    private BigDecimal total;

    public void calculateTotal() {
        this.total = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void validate() {
        if (items == null || items.isEmpty()) {
            throw new EmptyOrderException();
        }
    }

    // getters/setters
}