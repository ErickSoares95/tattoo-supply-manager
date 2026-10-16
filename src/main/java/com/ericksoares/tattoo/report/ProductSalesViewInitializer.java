package com.ericksoares.tattoo.report;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates/refreshes the "product_sales_report" VIEW on every startup, instead of a
 * one-off manual SQL script (the project has no Flyway/Liquibase yet - see CLAUDE.md).
 * This has to run as an ApplicationRunner, not a pre-startup script, because the VIEW
 * references order_items/payments/products, which only exist after Hibernate's
 * ddl-auto has run - in a fresh database (e.g. CI), those tables don't exist until
 * the app itself boots. DROP VIEW IF EXISTS + CREATE VIEW is idempotent, so re-running
 * this on every boot is safe.
 *
 * Revenue attribution: joins order_items to payments on order_id where status=APPROVED.
 * This relies on an invariant enforced by payment.ProcessPaymentService
 * (OrderAlreadyPaidException) - at most one APPROVED payment per order - so the join
 * never duplicates an order_item's row.
 */
@Slf4j
@Component
class ProductSalesViewInitializer implements ApplicationRunner {

    // Grouped by product_id only: order_items keeps a copy of the product name at order time,
    // so grouping by that copy would split a renamed product into two rows with the same id
    // (the entity's @Id). The current name wins; a deleted product keeps its last recorded one.
    private static final String CREATE_VIEW_SQL = """
            CREATE VIEW product_sales_report AS
            SELECT
                oi.product_id                                            AS product_id,
                COALESCE(MAX(pr.name), MAX(oi.product_name))             AS product_name,
                pr.stock                                                 AS current_stock,
                SUM(oi.quantity)                                         AS units_sold,
                SUM(oi.price * oi.quantity)                              AS revenue,
                RANK() OVER (ORDER BY SUM(oi.price * oi.quantity) DESC)  AS revenue_rank,
                ROUND(
                    100.0 * SUM(oi.price * oi.quantity)
                    / NULLIF(SUM(SUM(oi.price * oi.quantity)) OVER (), 0),
                2)                                                        AS revenue_share_pct
            FROM order_items oi
            JOIN payments p ON p.order_id = oi.order_id AND p.status = 'APPROVED'
            LEFT JOIN products pr ON pr.id = oi.product_id
            GROUP BY oi.product_id, pr.stock
            """;

    // DROP + CREATE instead of CREATE OR REPLACE: Postgres refuses OR REPLACE when a column's
    // type changes, and nothing depends on this view.
    private static final String DROP_VIEW_SQL = "DROP VIEW IF EXISTS product_sales_report";

    private final JdbcTemplate jdbcTemplate;

    public ProductSalesViewInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute(DROP_VIEW_SQL);
        jdbcTemplate.execute(CREATE_VIEW_SQL);
        log.info("product_sales_report view created/refreshed");
    }
}
