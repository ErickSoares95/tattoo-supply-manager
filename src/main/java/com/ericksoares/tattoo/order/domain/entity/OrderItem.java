package com.ericksoares.tattoo.order.domain.entity;

import com.ericksoares.tattoo.order.domain.exception.InvalidOrderItemQuantityException;
import com.ericksoares.tattoo.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class OrderItem extends BaseEntity {
    private Long productId;
    private String productName;
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