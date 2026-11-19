package com.ericksoares.tattoo.order;

import com.ericksoares.tattoo.product.StockConflictException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Entry point for registering an order. Deliberately not {@code @Transactional}: stock is
 * protected by an optimistic lock ({@code Product.version}), so when two orders race for the
 * same product the loser fails at commit with an OptimisticLockingFailureException. Each retry
 * calls {@link RegisterOrderTransaction}, which opens a new transaction and re-reads the
 * current stock - so a retry either succeeds or fails cleanly with InsufficientStockException.
 */
@Slf4j
@Service
class RegisterOrderService {

    static final int MAX_ATTEMPTS = 3;
    static final String IDEMPOTENCY_KEY_CONSTRAINT = "uk_orders_user_idempotency_key";

    private final RegisterOrderTransaction transaction;

    public RegisterOrderService(RegisterOrderTransaction transaction) {
        this.transaction = transaction;
    }

    public OrderResponse execute(OrderRequest request, Long userId) {
        return execute(request, userId, null);
    }

    /**
     * O5: with an Idempotency-Key, a retried request (double click, network retry) returns the
     * order the first request created instead of creating - and charging stock for - another.
     * Two concurrent requests with the same key both miss the lookup; the unique index
     * uk_orders_user_idempotency_key rejects the second insert, which then returns the first.
     */
    public OrderResponse execute(OrderRequest request, Long userId, String idempotencyKey) {

        if (idempotencyKey != null) {
            Optional<OrderResponse> existing = transaction.findByIdempotencyKey(userId, idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        for (int attempt = 1; ; attempt++) {
            try {
                return transaction.execute(request, userId, idempotencyKey);
            } catch (DataIntegrityViolationException e) {
                if (idempotencyKey != null && isIdempotencyKeyViolation(e)) {
                    return transaction.findByIdempotencyKey(userId, idempotencyKey).orElseThrow(() -> e);
                }
                throw e;
            } catch (OptimisticLockingFailureException e) {

                if (attempt >= MAX_ATTEMPTS) {
                    log.warn("Order for user {} gave up after {} optimistic-lock conflicts", userId, attempt);
                    throw new StockConflictException();
                }

                log.info("Optimistic-lock conflict registering order for user {} (attempt {}/{}), retrying",
                        userId, attempt, MAX_ATTEMPTS);
            }
        }
    }

    private static boolean isIdempotencyKeyViolation(DataIntegrityViolationException e) {
        return e.getMostSpecificCause().getMessage() != null
                && e.getMostSpecificCause().getMessage().contains(IDEMPOTENCY_KEY_CONSTRAINT);
    }
}
