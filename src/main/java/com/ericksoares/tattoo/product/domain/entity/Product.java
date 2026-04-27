package com.ericksoares.tattoo.product.domain.entity;

import com.ericksoares.tattoo.product.domain.exception.InsufficientStockException;
import com.ericksoares.tattoo.product.domain.exception.InvalidProductNameException;
import com.ericksoares.tattoo.product.domain.exception.InvalidProductPriceException;
import com.ericksoares.tattoo.product.domain.exception.InvalidStockException;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
