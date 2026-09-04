package com.ericksoares.tattoo.product.domain.entity;

import com.ericksoares.tattoo.product.domain.exception.InsufficientStockException;
import com.ericksoares.tattoo.product.domain.exception.InvalidProductNameException;
import com.ericksoares.tattoo.product.domain.exception.InvalidProductPriceException;
import com.ericksoares.tattoo.product.domain.exception.InvalidStockException;
import com.ericksoares.tattoo.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Product extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @Column(length = 500)
    private String description;

    @Column(length = 255)
    private String imageUrl;

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

    private static final int DAILY_DEAL_MONTHS_IN_STOCK = 3;

    // Business rule confirmed with the user (2026-09-05): a product enters "Ofertas do
    // dia" automatically once it's been sitting in the catalog/stock for 3+ months,
    // counted from creationDate (BaseEntity) - no separate "entered stock" timestamp or
    // batch/lot tracking exists (or is planned) yet, so creationDate doubles as that date.
    // Deliberately no discounted price here: this only decides *eligibility* for the
    // clearance shelf, it doesn't fabricate an oldPrice/discount the backend has no
    // record of.
    public boolean isOnDailyDeal() {
        return getCreationDate() != null
                && getCreationDate().isBefore(LocalDateTime.now().minusMonths(DAILY_DEAL_MONTHS_IN_STOCK));
    }
}
