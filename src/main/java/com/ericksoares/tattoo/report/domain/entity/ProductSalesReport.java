package com.ericksoares.tattoo.report.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import java.math.BigDecimal;

/**
 * Read-only projection backed by the "product_sales_report" Postgres VIEW (created/
 * refreshed on every boot by report.infrastructure.bootstrap.ProductSalesViewInitializer).
 *
 * Uses @Subselect instead of @Table on purpose: with a plain @Table(name=...), Hibernate's
 * ddl-auto=update runs BEFORE any ApplicationRunner and would create a real TABLE named
 * "product_sales_report" (since it has no idea a VIEW is coming later) - then the
 * ApplicationRunner's "CREATE OR REPLACE VIEW" fails with "is not a view", because you can't
 * replace a table with a view. @Subselect tells Hibernate this entity has no table to manage
 * at all (its rows come from the given query), so ddl-auto never touches it. @Synchronize
 * lists the underlying tables so Hibernate auto-flushes pending changes to them before this
 * entity is queried in the same session/transaction.
 */
@Entity
@Immutable
@Subselect("SELECT * FROM product_sales_report")
@Synchronize({"order_items", "payments", "products"})
@Getter
public class ProductSalesReport {

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
