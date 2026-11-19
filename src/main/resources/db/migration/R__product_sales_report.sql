-- Repeatable: Flyway re-runs this whenever the file changes.
-- DROP + CREATE instead of CREATE OR REPLACE: Postgres refuses OR REPLACE when a column's
-- type changes, and nothing depends on this view.
--
-- Revenue attribution: joins order_items to payments on order_id where status=APPROVED. At most
-- one APPROVED payment per order (unique index uk_payments_order_approved, V2), so the join
-- never duplicates an order_item's row.
--
-- Grouped by product_id only: order_items keeps a copy of the product name at order time,
-- so grouping by that copy would split a renamed product into two rows with the same id
-- (the entity's @Id). The current name wins; a deleted product keeps its last recorded one.
DROP VIEW IF EXISTS product_sales_report;

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
GROUP BY oi.product_id, pr.stock;
