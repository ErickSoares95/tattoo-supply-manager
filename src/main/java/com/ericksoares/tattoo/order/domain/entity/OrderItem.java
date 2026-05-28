package com.ericksoares.tattoo.order.domain.entity;

import com.ericksoares.tattoo.order.domain.exception.InvalidOrderItemQuantityException;
import com.ericksoares.tattoo.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {
    private Long productId;
    private Integer quantity;
    private BigDecimal price;

    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    public void validate() {
        if (quantity <= 0) {
            throw new InvalidOrderItemQuantityException();
        }
    }
}