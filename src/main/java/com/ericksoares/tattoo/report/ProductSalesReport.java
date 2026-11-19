package com.ericksoares.tattoo.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import java.math.BigDecimal;

/**
 * Read-only projection backed by the "product_sales_report" Postgres VIEW (Flyway repeatable
 * migration R__product_sales_report.sql).
 *
 * Uses @Subselect instead of @Table: Hibernate treats the entity as having no table of its own
 * (its rows come from the given query), so ddl-auto=validate never looks for a table named
 * "product_sales_report". @Synchronize
 * lists the underlying tables so Hibernate auto-flushes pending changes to them before this
 * entity is queried in the same session/transaction.
 */
@Entity
@Immutable
@Subselect("SELECT * FROM product_sales_report")
@Synchronize({"order_items", "payments", "products"})
@Getter
class ProductSalesReport {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "current_stock")
    private Integer currentStock;

    @Column(name = "units_sold")
    private Long unitsSold;

    private BigDecimal revenue;

    @Column(name = "revenue_rank")
    private Long revenueRank;

    @Column(name = "revenue_share_pct")
    private BigDecimal revenueSharePct;
}
