-- P7: units sold kept on the product (incremented when an order reserves stock, decremented
-- when it is cancelled) instead of summing all of order_items on every catalog listing.
-- Backfill = what the old query counted, minus orders that have since been cancelled.
ALTER TABLE products ADD COLUMN units_sold bigint NOT NULL DEFAULT 0;
UPDATE products p SET units_sold = s.total
  FROM (SELECT oi.product_id, SUM(oi.quantity) AS total
          FROM order_items oi JOIN orders o ON o.id = oi.order_id
         WHERE o.status <> 'CANCELLED'
         GROUP BY oi.product_id) s
 WHERE s.product_id = p.id;

-- P4: soft delete. Deleted products disappear from the catalog and can't be ordered, but the
-- row stays for order history and the sales report.
ALTER TABLE products ADD COLUMN deleted_at timestamp(6) with time zone;
