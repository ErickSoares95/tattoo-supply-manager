package com.ericksoares.tattoo.product.entity;

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

    public void decreaseStock(int quantity) {
        if (stock < quantity) {
            throw new RuntimeException("Estoque insuficiente");
        }
        this.stock -= quantity;
    }
}
