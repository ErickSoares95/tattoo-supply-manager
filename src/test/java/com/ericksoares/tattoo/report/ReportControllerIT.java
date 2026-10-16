package com.ericksoares.tattoo.report;

import com.ericksoares.tattoo.order.OrderSummary;
import com.ericksoares.tattoo.order.OrderTestFixtures;
import com.ericksoares.tattoo.payment.PaymentStatus;
import com.ericksoares.tattoo.payment.PaymentTestFixtures;
import com.ericksoares.tattoo.product.ProductSnapshot;
import com.ericksoares.tattoo.product.ProductTestFixtures;
import com.ericksoares.tattoo.shared.security.AuthenticatedUser;
import com.ericksoares.tattoo.user.UserTestFixtures;
import com.ericksoares.tattoo.user.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real "product_sales_report" VIEW (see ProductSalesViewInitializer) against
 * Postgres - window functions and joins can't be meaningfully verified with a mocked
 * repository, so this is a genuine end-to-end check, same philosophy as the other *IT classes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    private AuthenticatedUser createAdmin() {
        return UserTestFixtures.createUser(context, "report_admin@teste.com", UserType.ADMIN);
    }

    private ProductSnapshot createProduct(String name, BigDecimal price, Integer stock) {
        return ProductTestFixtures.createProduct(context, name, price, stock);
    }

    private OrderSummary createPaidOrder(ProductSnapshot product, int quantity) {

        OrderSummary savedOrder = OrderTestFixtures.saveOrder(
                context, 1L, product.id(), product.name(), quantity, product.price());

        PaymentTestFixtures.savePayment(
                context, savedOrder.id(), savedOrder.total(), "PIX", PaymentStatus.APPROVED);

        return savedOrder;
    }

    private RequestPostProcessor as(AuthenticatedUser principal) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        return authentication(auth);
    }

    @Test
    void shouldReturnUnauthorizedWhenNoToken() throws Exception {

        mockMvc.perform(get("/reports/product-sales"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReportRevenueAndRankForPaidOrdersOnly() throws Exception {

        AuthenticatedUser admin = createAdmin();

        ProductSnapshot paidProduct = createProduct("Report Tinta Black", BigDecimal.valueOf(45.90), 96);
        createPaidOrder(paidProduct, 2);

        ProductSnapshot unpaidProduct = createProduct("Report Agulha Sem Pagamento", BigDecimal.valueOf(12.50), 199);
        OrderTestFixtures.saveOrder(
                context, 1L, unpaidProduct.id(), unpaidProduct.name(), 5, unpaidProduct.price());

        // Note: assertions only rely on this test's own product - the shared dev/CI
        // database may already have revenue from other paid orders, so this can't
        // assume it's the only (or highest) revenue in the view (no fixed rank/100% share).
        mockMvc.perform(get("/reports/product-sales").with(as(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.productId == %d)]".formatted(paidProduct.id())).exists())
                .andExpect(jsonPath("$[?(@.productId == %d)].unitsSold".formatted(paidProduct.id())).value(2))
                .andExpect(jsonPath("$[?(@.productId == %d)].revenue".formatted(paidProduct.id())).value(91.80))
                .andExpect(jsonPath("$[?(@.productId == %d)].revenueRank".formatted(paidProduct.id())).exists())
                .andExpect(jsonPath("$[?(@.productId == %d)]".formatted(unpaidProduct.id())).doesNotExist());
    }

    @Test
    void shouldKeepOneRowPerProductAfterRename() throws Exception {

        AuthenticatedUser admin = createAdmin();

        ProductSnapshot product = createProduct("Report Nome Atual", BigDecimal.valueOf(10.00), 50);

        // order_items keeps the name at order time: the first order recorded the old name.
        OrderSummary oldNameOrder = OrderTestFixtures.saveOrder(
                context, 1L, product.id(), "Report Nome Antigo", 1, product.price());
        PaymentTestFixtures.savePayment(
                context, oldNameOrder.id(), oldNameOrder.total(), "PIX", PaymentStatus.APPROVED);
        createPaidOrder(product, 2);

        mockMvc.perform(get("/reports/product-sales").with(as(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.productId == %d)]".formatted(product.id()), hasSize(1)))
                .andExpect(jsonPath("$[?(@.productId == %d)].unitsSold".formatted(product.id())).value(3))
                .andExpect(jsonPath("$[?(@.productId == %d)].productName".formatted(product.id())).value("Report Nome Atual"));
    }
}
