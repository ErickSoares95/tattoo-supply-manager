package com.ericksoares.tattoo.order;

import com.ericksoares.tattoo.product.ProductCatalog;
import com.ericksoares.tattoo.product.ProductSnapshot;
import com.ericksoares.tattoo.product.ProductTestFixtures;
import com.ericksoares.tattoo.product.InsufficientStockException;
import com.ericksoares.tattoo.product.StockConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real concurrency against Postgres, so deliberately not @Transactional: each thread must
 * run its own transaction for the optimistic lock on Product.version to come into play.
 */
@SpringBootTest
class RegisterOrderConcurrencyIT {

    private static final long USER_ID = 987_654L;

    @Autowired
    private RegisterOrderService service;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ProductCatalog productCatalog;

    @Autowired
    private OrderRepository orderRepository;

    private final List<Long> createdOrderIds = new CopyOnWriteArrayList<>();
    private Long productId;

    @AfterEach
    void cleanUp() {
        orderRepository.deleteAllById(createdOrderIds);
        if (productId != null) {
            ProductTestFixtures.deleteProducts(context, List.of(productId));
        }
    }

    private ProductSnapshot createProduct(int stock) {
        ProductSnapshot product = ProductTestFixtures.createProduct(
                context, "Concurrency Ink " + System.nanoTime(), BigDecimal.TEN, stock);
        productId = product.id();
        return product;
    }

    /** Runs {@code threads} single-unit orders for the product at the same instant. */
    private List<Object> placeOrdersConcurrently(int threads) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CyclicBarrier startTogether = new CyclicBarrier(threads);
        List<Future<Object>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                startTogether.await(10, TimeUnit.SECONDS);
                try {
                    OrderResponse response = service.execute(
                            new OrderRequest(List.of(new OrderItemRequest(productId, 1))),
                            USER_ID
                    );
                    createdOrderIds.add(response.id());
                    return response;
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
    void shouldLetOnlyOneOfTwoConcurrentOrdersTakeTheLastUnit() throws Exception {

        createProduct(1);

        List<Object> results = placeOrdersConcurrently(2);

        long succeeded = results.stream().filter(r -> r instanceof OrderResponse).count();
        List<Object> failed = results.stream().filter(r -> !(r instanceof OrderResponse)).toList();

        assertEquals(1, succeeded, "exactly one order may take the last unit: " + results);
        assertEquals(1, failed.size());
        assertInstanceOf(InsufficientStockException.class, failed.get(0));

        assertEquals(0, productCatalog.findById(productId).orElseThrow().stock());
    }

    @Test
    void shouldNeverOversellUnderHeavierContention() throws Exception {

        int stock = 3;
        int threads = 8;
        createProduct(stock);

        List<Object> results = placeOrdersConcurrently(threads);

        long succeeded = results.stream().filter(r -> r instanceof OrderResponse).count();

        // Losers either saw the stock run out or exhausted their optimistic-lock retries (409).
        results.stream()
                .filter(r -> !(r instanceof OrderResponse))
                .forEach(r -> assertTrue(
                        r instanceof InsufficientStockException || r instanceof StockConflictException,
                        "unexpected failure: " + r
                ));

        int finalStock = productCatalog.findById(productId).orElseThrow().stock();

        assertTrue(succeeded <= stock);
        assertTrue(finalStock >= 0);
        assertEquals(stock - succeeded, finalStock);
    }
}
