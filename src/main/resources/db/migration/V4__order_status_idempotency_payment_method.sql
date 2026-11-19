-- O4: order lifecycle. Existing orders with an approved payment are PAID, the rest wait for
-- payment (and expire like new ones). version = optimistic lock, so paying and cancelling
-- the same order at the same time can't both succeed.
ALTER TABLE orders ADD COLUMN status varchar(20) NOT NULL DEFAULT 'PENDING_PAYMENT'
    CONSTRAINT orders_status_check CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'CANCELLED'));
ALTER TABLE orders ADD COLUMN version bigint NOT NULL DEFAULT 0;
UPDATE orders o SET status = 'PAID'
 WHERE EXISTS (SELECT 1 FROM payments p WHERE p.order_id = o.id AND p.status = 'APPROVED');

-- O5: a retried POST /orders with the same Idempotency-Key returns the first order.
ALTER TABLE orders ADD COLUMN idempotency_key varchar(100);
CREATE UNIQUE INDEX uk_orders_user_idempotency_key
    ON orders (user_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

-- Y3: payment method is a closed set. NOT VALID: enforced for new rows only, history kept as is.
ALTER TABLE payments ADD CONSTRAINT payments_method_check
    CHECK (method IN ('PIX', 'CREDIT_CARD', 'DEBIT_CARD', 'BOLETO')) NOT VALID;
