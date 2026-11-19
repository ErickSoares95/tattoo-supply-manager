package com.ericksoares.tattoo.payment;

import com.ericksoares.tattoo.order.OrderSummary;
import com.ericksoares.tattoo.order.OrderTestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real concurrency against Postgres, so deliberately not @Transactional: each thread must run
 * its own transaction for the unique index uk_payments_order_approved (migration V2) to decide.
 */
@SpringBootTest
class ProcessPaymentConcurrencyIT {

    private static final long USER_ID = 876_543L;

    @Autowired
    private ProcessPaymentService service;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ApplicationContext context;

    private OrderSummary order;

    @BeforeEach
    void createOrder() {
        // product_id has no FK (cross-module decoupling), so no product is needed here.
        order = OrderTestFixtures.saveOrder(context, USER_ID, 999_999L, "Concurrency Ink", 1, BigDecimal.TEN);
    }

    @AfterEach
    void cleanUp() {
        paymentRepository.deleteAll(paymentRepository.findByOrderIdOrderByCreationDateDesc(order.id()));
        OrderTestFixtures.deleteOrders(context, List.of(order.id()));
    }

    /** Pays the order with its exact total from {@code threads} threads at the same instant. */
    private List<Object> payConcurrently(int threads) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CyclicBarrier startTogether = new CyclicBarrier(threads);
        List<Future<Object>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                startTogether.await(10, TimeUnit.SECONDS);
                try {
                    return service.execute(order.id(), new PaymentRequest(order.total(), PaymentMethod.PIX), USER_ID, false);
                } catch (Exception e) {
                    return e;
                }
            }));
        }

        List<Object> results = new ArrayList<>();
        for (Future<Object> future : futures) {
            results.add(future.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return results;
    }

    @Test
    void shouldApproveOnlyOneOfManyConcurrentPayments() throws Exception {

        int threads = 8;

        List<Object> results = payConcurrently(threads);

        long approved = results.stream()
                .filter(r -> r instanceof PaymentResponse response && response.status() == PaymentStatus.APPROVED)
                .count();

        // Losers either saw the approved payment in the check or hit the unique index: both are 409.
        results.stream()
                .filter(r -> !(r instanceof PaymentResponse))
                .forEach(r -> assertInstanceOf(OrderAlreadyPaidException.class, r, "unexpected failure: " + r));

        assertEquals(1, approved, "exactly one payment may be approved: " + results);
        assertEquals(1, paymentRepository.findByOrderIdOrderByCreationDateDesc(order.id()).stream()
                .filter(Payment::isApproved)
                .count());
    }

    @Test
    void shouldRejectSecondApprovedRowAtDatabaseLevel() {

        PaymentTestFixtures.savePayment(context, order.id(), order.total(), "PIX", PaymentStatus.APPROVED);

        assertThrows(DataIntegrityViolationException.class, () -> PaymentTestFixtures.savePayment(
                context, order.id(), order.total(), "PIX", PaymentStatus.APPROVED));
    }

    @Test
    void shouldStillAllowRejectedPaymentsAlongsideTheApprovedOne() {

        PaymentTestFixtures.savePayment(context, order.id(), BigDecimal.ONE, "PIX", PaymentStatus.REJECTED);
        PaymentTestFixtures.savePayment(context, order.id(), BigDecimal.ONE, "PIX", PaymentStatus.REJECTED);

        assertDoesNotThrow(() -> PaymentTestFixtures.savePayment(
                context, order.id(), order.total(), "PIX", PaymentStatus.APPROVED));
    }
}
