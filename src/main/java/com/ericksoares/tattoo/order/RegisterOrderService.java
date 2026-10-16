package com.ericksoares.tattoo.order;

import com.ericksoares.tattoo.product.StockConflictException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

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

    private final RegisterOrderTransaction transaction;

    public RegisterOrderService(RegisterOrderTransaction transaction) {
        this.transaction = transaction;
    }

    public OrderResponse execute(OrderRequest request, Long userId) {

        for (int attempt = 1; ; attempt++) {
            try {
                return transaction.execute(request, userId);
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
}
