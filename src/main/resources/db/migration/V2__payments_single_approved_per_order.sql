-- At most one APPROVED payment per order. ProcessPaymentService checks this before inserting,
-- but two concurrent requests could both pass the check; this index makes the second insert
-- fail (ProcessPaymentService turns that into OrderAlreadyPaidException, HTTP 409).
-- product_sales_report joins payments on order_id and relies on this to never double revenue.
-- Fails on purpose if a database already holds duplicates: resolve them before migrating.
CREATE UNIQUE INDEX uk_payments_order_approved
    ON payments (order_id)
    WHERE status = 'APPROVED';
