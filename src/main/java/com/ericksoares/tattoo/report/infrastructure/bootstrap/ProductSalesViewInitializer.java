package com.ericksoares.tattoo.report.infrastructure.bootstrap;

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
 * the app itself boots. CREATE OR REPLACE VIEW is idempotent, so re-running this on
 * every boot is safe.
 *
 * Revenue attribution: joins order_items to payments on order_id where status=APPROVED.
 * This relies on an invariant enforced by payment.application.service.ProcessPaymentService
 * (OrderAlreadyPaidException) - at most one APPROVED payment per order - so the join
 * never duplicates an order_item's row.
 */
@Slf4j
@Component
public class ProductSalesViewInitializer implements ApplicationRunner {

    private static final String CREATE_VIEW_SQL = """
            CREATE OR REPLACE VIEW product_sales_report AS
            SELECT
                oi.product_id                                            AS product_id,
                oi.product_name                                          AS product_name,
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
            GROUP BY oi.product_id, oi.product_name, pr.stock
            """;

    private final JdbcTemplate jdbcTemplate;

    public ProductSalesViewInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute(CREATE_VIEW_SQL);
        log.info("product_sales_report view created/refreshed");
    }
}
