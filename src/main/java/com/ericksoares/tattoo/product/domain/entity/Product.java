package com.ericksoares.tattoo.product.domain.entity;

import com.ericksoares.tattoo.product.domain.exception.InsufficientStockException;
import com.ericksoares.tattoo.product.domain.exception.InvalidProductNameException;
import com.ericksoares.tattoo.product.domain.exception.InvalidProductPriceException;
import com.ericksoares.tattoo.product.domain.exception.InvalidStockException;
import com.ericksoares.tattoo.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Product extends BaseEntity {

    private String name;
    private BigDecimal price;
    private Integer stock;

    public void validate() {

        if (name == null || name.isBlank()) {
            throw new InvalidProductNameException();
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidProductPriceException();
        }

        if (stock == null || stock < 0) {
            throw new InvalidStockException();
        }
    }

    public void decreaseStock(int quantity) {
        if (stock < quantity) {
            throw new InsufficientStockException(this.name);
        }
        this.stock -= quantity;
    }
}
