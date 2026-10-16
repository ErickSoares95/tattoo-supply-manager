package com.ericksoares.tattoo.product;

import com.ericksoares.tattoo.shared.persistence.BaseEntity;
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
class Product extends BaseEntity {

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

    // Nullable on purpose, unlike the rest of Product's required fields: the column was
    // added after 5 real products already existed in production with no category concept
    // at all (menu links all pointed at the same unfiltered /produtos before this). Bean
    // Validation's @NotNull on ProductRequest/UpdateProductRequest is what actually
    // enforces this for admin create/update from here on - validate() below stays
    // lenient about it so the pre-existing rows don't start failing on their next save
    // before someone backfills them.
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProductCategory category;

    // Optimistic lock for concurrent stock changes (two orders reading the same stock):
    // the second UPDATE sees a stale version and fails instead of overselling. Boxed Long and
    // "default 0" so ddl-auto=update can add the column to a table that already has rows
    // (existing products get version 0); see RegisterOrderService for the retry.
    @Version
    @Column(columnDefinition = "bigint default 0")
    private Long version;

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
        if (quantity <= 0) {
            throw new InvalidStockQuantityException();
        }

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
