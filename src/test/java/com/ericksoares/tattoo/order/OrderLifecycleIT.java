package com.ericksoares.tattoo.order;

import com.ericksoares.tattoo.product.ProductCatalog;
import com.ericksoares.tattoo.product.ProductSnapshot;
import com.ericksoares.tattoo.product.ProductTestFixtures;
import com.ericksoares.tattoo.shared.security.AuthenticatedUser;
import com.ericksoares.tattoo.user.UserTestFixtures;
import com.ericksoares.tattoo.user.UserType;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O4 (status, cancel, expiration), O5 (Idempotency-Key) and O2 (no N+1 listing orders). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderLifecycleIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductCatalog productCatalog;

    @Autowired
    private OrderExpirationJob expirationJob;

    @Autowired
    private FindOrdersService findOrdersService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private RequestPostProcessor as(AuthenticatedUser principal) {
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private AuthenticatedUser client(String name) {
        return UserTestFixtures.createUser(context, name + System.nanoTime() + "@test.com", UserType.CLIENT);
    }

    private ProductSnapshot product(int stock) {
        return ProductTestFixtures.createProduct(context, "Lifecycle Ink " + System.nanoTime(), BigDecimal.TEN, stock);
    }

    private String orderBody(Long productId, int quantity) {
        return """
                {"items":[{"productId":%d,"quantity":%d}]}
                """.formatted(productId, quantity);
    }

    private Long placeOrder(AuthenticatedUser user, Long productId, int quantity) throws Exception {
        String body = mockMvc.perform(post("/orders").with(as(user))
                        .contentType(MediaType.APPLICATION_JSON).content(orderBody(productId, quantity)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andReturn().getResponse().getContentAsString();
        return Long.valueOf(body.replaceAll("^\\{\"id\":(\\d+).*", "$1"));
    }

    private int stockOf(Long productId) {
        return productCatalog.findById(productId).orElseThrow().stock();
    }

    @Test
    void ownerCancelsAnUnpaidOrderAndGetsTheStockBack() throws Exception {
        AuthenticatedUser owner = client("owner");
        ProductSnapshot ink = product(10);
        Long orderId = placeOrder(owner, ink.id(), 3);
        assertEquals(7, stockOf(ink.id()));

        mockMvc.perform(post("/orders/" + orderId + "/cancel").with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertEquals(10, stockOf(ink.id()));

        // Cancelling twice would give the stock back twice.
        mockMvc.perform(post("/orders/" + orderId + "/cancel").with(as(owner)))
                .andExpect(status().isConflict());
        assertEquals(10, stockOf(ink.id()));
    }

    @Test
    void anotherClientCannotCancelTheOrder() throws Exception {
        ProductSnapshot ink = product(10);
        Long orderId = placeOrder(client("owner"), ink.id(), 1);

        mockMvc.perform(post("/orders/" + orderId + "/cancel").with(as(client("intruder"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void aPaidOrderCannotBeCancelled() throws Exception {
        AuthenticatedUser owner = client("payer");
        ProductSnapshot ink = product(10);
        Long orderId = placeOrder(owner, ink.id(), 1);

        mockMvc.perform(post("/orders/" + orderId + "/payments").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":10.00,\"method\":\"PIX\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        assertEquals(OrderStatus.PAID, orderRepository.findById(orderId).orElseThrow().getStatus());

        mockMvc.perform(post("/orders/" + orderId + "/cancel").with(as(owner)))
                .andExpect(status().isConflict());
    }

    @Test
    void theJobCancelsUnpaidOrdersOlderThanTheExpirationAndGivesTheStockBack() throws Exception {
        AuthenticatedUser owner = client("late");
        ProductSnapshot ink = product(10);
        Long expired = placeOrder(owner, ink.id(), 2);
        Long recent = placeOrder(owner, ink.id(), 1);
        orderRepository.flush();
        jdbcTemplate.update("update orders set creation_date = now() - interval '3 days' where id = ?", expired);

        expirationJob.cancelExpiredOrders();

        assertEquals(OrderStatus.CANCELLED, statusOf(expired));
        assertEquals(OrderStatus.PENDING_PAYMENT, statusOf(recent));
        assertEquals(9, stockOf(ink.id()));
    }

    private OrderStatus statusOf(Long orderId) {
        return jdbcTemplate.queryForObject("select status from orders where id = ?", OrderStatus.class, orderId);
    }

    @Test
    void aRetriedRequestWithTheSameIdempotencyKeyReturnsTheFirstOrder() throws Exception {
        AuthenticatedUser owner = client("retry");
        ProductSnapshot ink = product(10);

        String first = mockMvc.perform(post("/orders").with(as(owner)).header("Idempotency-Key", "checkout-123")
                        .contentType(MediaType.APPLICATION_JSON).content(orderBody(ink.id(), 2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(post("/orders").with(as(owner)).header("Idempotency-Key", "checkout-123")
                        .contentType(MediaType.APPLICATION_JSON).content(orderBody(ink.id(), 2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertEquals(first.replaceAll("^\\{\"id\":(\\d+).*", "$1"), second.replaceAll("^\\{\"id\":(\\d+).*", "$1"));
        assertEquals(8, stockOf(ink.id()), "stock is reserved once");

        // Same key from another user is another order.
        mockMvc.perform(post("/orders").with(as(client("other"))).header("Idempotency-Key", "checkout-123")
                        .contentType(MediaType.APPLICATION_JSON).content(orderBody(ink.id(), 1)))
                .andExpect(status().isCreated());
        assertEquals(7, stockOf(ink.id()));
    }

    @Test
    void anOverlongIdempotencyKeyIsRejected() throws Exception {
        ProductSnapshot ink = product(10);

        mockMvc.perform(post("/orders").with(as(client("long"))).header("Idempotency-Key", "x".repeat(101))
                        .contentType(MediaType.APPLICATION_JSON).content(orderBody(ink.id(), 1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listingOrdersLoadsTheItemsInBatchInsteadOfOneQueryPerOrder() throws Exception {
        AuthenticatedUser owner = client("lister");
        ProductSnapshot ink = product(20);
        for (int i = 0; i < 5; i++) {
            placeOrder(owner, ink.id(), 1);
        }
        orderRepository.flush();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        // Detach everything so the items really come from the database.
        context.getBean(jakarta.persistence.EntityManager.class).clear();
        statistics.clear();

        var page = findOrdersService.execute(owner.getId(), false, PageRequest.of(0, 20));

        assertEquals(5, page.getContent().size());
        assertTrue(page.getContent().stream().allMatch(order -> order.items().size() == 1));
        // page query + count query + one batched items query (was 1 + 1 + 5 before @BatchSize)
        assertTrue(statistics.getPrepareStatementCount() <= 3,
                "expected at most 3 statements, got " + statistics.getPrepareStatementCount());
    }
}
