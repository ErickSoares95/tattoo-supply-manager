package com.ericksoares.tattoo.notification.application.service;

import com.ericksoares.tattoo.notification.domain.entity.FailedNotification;
import com.ericksoares.tattoo.notification.domain.entity.FailedPaymentNotification;
import com.ericksoares.tattoo.notification.domain.repository.FailedNotificationRepository;
import com.ericksoares.tattoo.notification.domain.repository.FailedPaymentNotificationRepository;
import com.ericksoares.tattoo.notification.domain.repository.ProcessedPaymentEventRepository;
import com.ericksoares.tattoo.notification.event.kafka.PaymentNotificationListener;
import com.ericksoares.tattoo.notification.infrastructure.email.EmailNotificationSender;
import com.ericksoares.tattoo.notification.infrastructure.webhook.WebhookNotificationSender;
import com.ericksoares.tattoo.notification.infrastructure.whatsapp.WhatsAppNotificationSender;
import com.ericksoares.tattoo.order.application.dto.OrderItemRequest;
import com.ericksoares.tattoo.order.application.dto.OrderRequest;
import com.ericksoares.tattoo.order.application.dto.OrderResponse;
import com.ericksoares.tattoo.order.application.service.RegisterOrderService;
import com.ericksoares.tattoo.order.infrasctruture.repository.OrderRepository;
import com.ericksoares.tattoo.payment.domain.entity.PaymentStatus;
import com.ericksoares.tattoo.payment.domain.event.PaymentProcessedEvent;
import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * End-to-end checks of the failure/retry flow against real Postgres, with the three senders
 * as Mockito spies so a channel can be made to fail. Deliberately not @Transactional: the
 * listener, executor and reprocess services each run their own transactions.
 *
 * <p>The scheduled job is parked in every test context (src/test/resources/application.properties,
 * huge delays) so tests drive the reprocess services explicitly. Failure rows are created with a future {@code nextAttemptAt}, so a job
 * of another cached test context cannot pick them up while a test runs.
 */
@SpringBootTest(properties = {
        "notification.retry.fixed-delay-ms=3600000",
        "notification.retry.initial-delay-ms=3600000"
})
class NotificationRetryFlowIT {

    private static final long USER_ID = 987_655L;
    private static final long PAYMENT_ORDER_ID = 912_345L;
    private static final long CONCURRENT_ORDER_ID = 912_346L;

    @SpyBean
    private WebhookNotificationSender webhook;

    @SpyBean
    private EmailNotificationSender email;

    @SpyBean
    private WhatsAppNotificationSender whatsapp;

    @Autowired
    private RegisterOrderService orderService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private FailedNotificationRepository failedRepository;
    @Autowired
    private FailedPaymentNotificationRepository failedPaymentRepository;
    @Autowired
    private ProcessedPaymentEventRepository processedPaymentEventRepository;
    @Autowired
    private NotificationReprocessService reprocessService;
    @Autowired
    private PaymentNotificationReprocessService paymentReprocessService;
    @Autowired
    private PaymentNotificationListener paymentListener;

    private final List<Long> orderIds = new ArrayList<>();
    private final List<Long> productIds = new ArrayList<>();
    private final List<UUID> eventIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        List<Long> relatedOrders = new ArrayList<>(orderIds);
        relatedOrders.add(PAYMENT_ORDER_ID);
        relatedOrders.add(CONCURRENT_ORDER_ID);

        failedRepository.deleteAll(failedRepository.findAll().stream()
                .filter(f -> relatedOrders.contains(f.getOrderId())).toList());
        failedPaymentRepository.deleteAll(failedPaymentRepository.findAll().stream()
                .filter(f -> relatedOrders.contains(f.getOrderId())).toList());
        eventIds.forEach(id -> processedPaymentEventRepository.findAll().stream()
                .filter(e -> id.equals(e.getEventId()))
                .forEach(processedPaymentEventRepository::delete));

        orderRepository.deleteAllById(orderIds);
        productRepository.deleteAllById(productIds);
    }

    private OrderResponse placeOrderWithThreeItems() {
        List<OrderItemRequest> items = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Product product = productRepository.save(Product.builder()
                    .name("Retry Flow Ink " + i + " " + System.nanoTime())
                    .price(BigDecimal.TEN)
                    .stock(10)
                    .build());
            productIds.add(product.getId());
            items.add(new OrderItemRequest(product.getId(), 1));
        }
        OrderResponse order = orderService.execute(new OrderRequest(items), USER_ID);
        orderIds.add(order.id());
        return order;
    }

    private List<FailedNotification> failuresOf(Long orderId) {
        return failedRepository.findAll().stream().filter(f -> orderId.equals(f.getOrderId())).toList();
    }

    private List<FailedPaymentNotification> paymentFailuresOf(Long orderId) {
        return failedPaymentRepository.findAll().stream().filter(f -> orderId.equals(f.getOrderId())).toList();
    }

    private void failWebhookOnOrderRegistered(RuntimeException error) {
        doThrow(error).when(webhook).sendOrderRegistered(anyLong());
    }

    @Test
    void transientWebhookFailureCreatesASingleWebhookRowAndOtherSendersDeliverOnce() {

        failWebhookOnOrderRegistered(new UncheckedIOException(new IOException("webhook down")));

        Long orderId = placeOrderWithThreeItems().id();

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertEquals(1, failuresOf(orderId).size()));

        FailedNotification row = failuresOf(orderId).get(0);
        assertEquals("WEBHOOK", row.getSenderName());
        assertEquals(1, row.getAttempts());
        assertFalse(row.getProcessed());
        assertNotNull(row.getNextAttemptAt());

        // One notification per order (not per item), and the healthy channels are not repeated.
        verify(email, times(1)).sendOrderRegistered(orderId);
        verify(whatsapp, times(1)).sendOrderRegistered(orderId);
        verify(webhook, times(1)).sendOrderRegistered(orderId);
    }

    @Test
    void reprocessResendsOnlyThroughWebhookAndClearsTheFailure() {

        failWebhookOnOrderRegistered(new UncheckedIOException(new IOException("webhook down")));
        Long orderId = placeOrderWithThreeItems().id();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertEquals(1, failuresOf(orderId).size()));

        // The webhook recovers.
        doNothing().when(webhook).sendOrderRegistered(anyLong());

        reprocessService.reprocessAll();

        assertTrue(failuresOf(orderId).get(0).getProcessed());
        assertNull(failuresOf(orderId).get(0).getLockedUntil());

        verify(webhook, times(2)).sendOrderRegistered(orderId);
        verify(email, times(1)).sendOrderRegistered(orderId);
        verify(whatsapp, times(1)).sendOrderRegistered(orderId);
    }

    @Test
    void permanentFailureStaysTerminalAndIsNeverResent() {

        failWebhookOnOrderRegistered(new IllegalArgumentException("invalid payload"));
        Long orderId = placeOrderWithThreeItems().id();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertEquals(1, failuresOf(orderId).size()));

        FailedNotification row = failuresOf(orderId).get(0);
        assertEquals(NotificationRetryPolicy.MAX_ATTEMPTS, row.getAttempts());
        assertNull(row.getNextAttemptAt());
        assertFalse(row.getProcessed());

        reprocessService.reprocessAll();
        reprocessService.reprocessDue();

        verify(webhook, times(1)).sendOrderRegistered(orderId);
        assertFalse(failuresOf(orderId).get(0).getProcessed());
    }

    @Test
    void twoConcurrentReprocessRunsOnTheSameRowSendExactlyOnce() throws Exception {

        FailedNotification row = new FailedNotification();
        row.setOrderId(CONCURRENT_ORDER_ID);
        row.setSenderName("WEBHOOK");
        row.setCreatedAt(LocalDateTime.now());
        row.setProcessed(false);
        row.setAttempts(1);
        row.setNextAttemptAt(Instant.now().plusSeconds(600));
        failedRepository.save(row);

        // Slow send, so the second run overlaps the first one.
        doAnswer(invocation -> {
            Thread.sleep(500);
            return null;
        }).when(webhook).sendOrderRegistered(CONCURRENT_ORDER_ID);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier startTogether = new CyclicBarrier(2);
        List<Future<?>> runs = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            runs.add(pool.submit(() -> {
                startTogether.await(10, TimeUnit.SECONDS);
                reprocessService.reprocessAll();
                return null;
            }));
        }
        for (Future<?> run : runs) {
            run.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        verify(webhook, times(1)).sendOrderRegistered(CONCURRENT_ORDER_ID);
        assertTrue(failuresOf(CONCURRENT_ORDER_ID).get(0).getProcessed());
    }

    @Test
    void approvedAndRejectedPaymentFailuresCreateOneRowPerSenderAndTheJobResendsThem() {

        for (PaymentStatus status : List.of(PaymentStatus.APPROVED, PaymentStatus.REJECTED)) {
            failedPaymentRepository.deleteAll(paymentFailuresOf(PAYMENT_ORDER_ID));
            reset(webhook, email, whatsapp);

            UncheckedIOException down = new UncheckedIOException(new IOException("down"));
            doThrow(down).when(webhook).sendPaymentConfirmed(anyLong());
            doThrow(down).when(webhook).sendPaymentRejected(anyLong());
            doThrow(down).when(whatsapp).sendPaymentConfirmed(anyLong());
            doThrow(down).when(whatsapp).sendPaymentRejected(anyLong());

            UUID eventId = UUID.randomUUID();
            eventIds.add(eventId);

            paymentListener.handle(new PaymentProcessedEvent(
                    eventId, PAYMENT_ORDER_ID, status, BigDecimal.TEN, LocalDateTime.now()));

            List<FailedPaymentNotification> rows = paymentFailuresOf(PAYMENT_ORDER_ID);
            assertEquals(2, rows.size(), "one row per failed sender for " + status);
            assertEquals(List.of("WEBHOOK", "WHATSAPP"),
                    rows.stream().map(FailedPaymentNotification::getSenderName).sorted().toList());
            rows.forEach(r -> {
                assertEquals(status.name(), r.getStatus());
                assertEquals(eventId, r.getEventId());
                assertEquals(1, r.getAttempts());
                assertFalse(r.getProcessed());
            });

            // Senders recover and the backoff elapses: the scheduled job replays from the table.
            doNothing().when(webhook).sendPaymentConfirmed(anyLong());
            doNothing().when(webhook).sendPaymentRejected(anyLong());
            doNothing().when(whatsapp).sendPaymentConfirmed(anyLong());
            doNothing().when(whatsapp).sendPaymentRejected(anyLong());
            rows.forEach(r -> {
                r.setNextAttemptAt(Instant.now().minusSeconds(1));
                failedPaymentRepository.save(r);
            });

            // Called directly (the scheduled job is parked in test contexts): deterministic, no race.
            paymentReprocessService.reprocessDue();

            paymentFailuresOf(PAYMENT_ORDER_ID).forEach(r -> assertTrue(r.getProcessed()));

            if (status == PaymentStatus.APPROVED) {
                verify(email, times(1)).sendPaymentConfirmed(PAYMENT_ORDER_ID);
                verify(webhook, times(2)).sendPaymentConfirmed(PAYMENT_ORDER_ID);
                verify(whatsapp, times(2)).sendPaymentConfirmed(PAYMENT_ORDER_ID);
                verify(webhook, never()).sendPaymentRejected(anyLong());
            } else {
                verify(email, times(1)).sendPaymentRejected(PAYMENT_ORDER_ID);
                verify(webhook, times(2)).sendPaymentRejected(PAYMENT_ORDER_ID);
                verify(whatsapp, times(2)).sendPaymentRejected(PAYMENT_ORDER_ID);
                verify(webhook, never()).sendPaymentConfirmed(anyLong());
            }
        }
    }
}
