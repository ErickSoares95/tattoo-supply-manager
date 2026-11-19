package com.ericksoares.tattoo.order;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * O4: an order that is never paid would hold its stock forever. Every few minutes this
 * cancels PENDING_PAYMENT orders older than {@code order.expiration.hours} (default 48) and
 * gives their stock back. Each order is cancelled in its own transaction, so one failure
 * (e.g. a payment approved at the same instant) doesn't stop the rest; it is retried next run.
 */
@Slf4j
@Component
class OrderExpirationJob {

    private final OrderRepository repository;
    private final CancelOrderService cancelOrderService;
    private final long expirationHours;

    OrderExpirationJob(OrderRepository repository, CancelOrderService cancelOrderService,
                       @Value("${order.expiration.hours:48}") long expirationHours) {
        this.repository = repository;
        this.cancelOrderService = cancelOrderService;
        this.expirationHours = expirationHours;
    }

    @Scheduled(fixedDelayString = "${order.expiration.fixed-delay-ms:600000}",
            initialDelayString = "${order.expiration.initial-delay-ms:60000}")
    public void cancelExpiredOrders() {
        List<Long> expired = repository.findIdsByStatusAndCreationDateBefore(
                OrderStatus.PENDING_PAYMENT, LocalDateTime.now().minusHours(expirationHours));

        for (Long orderId : expired) {
            try {
                cancelOrderService.cancelExpired(orderId);
                log.info("Order {} expired unpaid after {}h and was cancelled", orderId, expirationHours);
            } catch (RuntimeException e) {
                log.warn("Could not expire order {} (will retry next run): {}", orderId, e.getMessage());
            }
        }
    }
}
