package com.ericksoares.tattoo.report.presentation;

import com.ericksoares.tattoo.order.domain.entity.Order;
import com.ericksoares.tattoo.order.domain.entity.OrderItem;
import com.ericksoares.tattoo.order.infrasctruture.repository.OrderRepository;
import com.ericksoares.tattoo.payment.domain.entity.Payment;
import com.ericksoares.tattoo.payment.domain.entity.PaymentStatus;
import com.ericksoares.tattoo.payment.infrastructure.repository.PaymentRepository;
import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import com.ericksoares.tattoo.shared.security.model.AuthenticatedUser;
import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.enums.UserStatus;
import com.ericksoares.tattoo.user.domain.enums.UserType;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private User createAdmin() {
        User user = User.builder()
                .username("report_admin")
                .email("report_admin@teste.com")
                .password(passwordEncoder.encode("Senha1234"))
                .fullName("Report Admin")
                .userStatus(UserStatus.ACTIVE)
                .userType(UserType.ADMIN)
                .build();

        return userRepository.save(user);
    }

    private Product createProduct(String name, BigDecimal price, Integer stock) {
        return productRepository.save(
                Product.builder().name(name).price(price).stock(stock).build()
        );
    }

    private Order createPaidOrder(Product product, int quantity) {

        OrderItem item = new OrderItem();
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setQuantity(quantity);
        item.setPrice(product.getPrice());

        Order order = new Order();
        order.setUserId(1L);
        order.setItems(List.of(item));
        order.calculateTotal();

        Order savedOrder = orderRepository.save(order);

        Payment payment = Payment.builder()
                .orderId(savedOrder.getId())
                .amount(savedOrder.getTotal())
                .method("PIX")
                .status(PaymentStatus.APPROVED)
                .build();

        paymentRepository.save(payment);

        return savedOrder;
    }

    private RequestPostProcessor as(User user) {
        AuthenticatedUser principal = new AuthenticatedUser(user);
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

        User admin = createAdmin();

        Product paidProduct = createProduct("Report Tinta Black", BigDecimal.valueOf(45.90), 96);
        createPaidOrder(paidProduct, 2);

        Product unpaidProduct = createProduct("Report Agulha Sem Pagamento", BigDecimal.valueOf(12.50), 199);
        OrderItem unpaidItem = new OrderItem();
        unpaidItem.setProductId(unpaidProduct.getId());
        unpaidItem.setProductName(unpaidProduct.getName());
        unpaidItem.setQuantity(5);
        unpaidItem.setPrice(unpaidProduct.getPrice());
        Order unpaidOrder = new Order();
        unpaidOrder.setUserId(1L);
        unpaidOrder.setItems(List.of(unpaidItem));
        unpaidOrder.calculateTotal();
        orderRepository.save(unpaidOrder);

        // Note: assertions only rely on this test's own product - the shared dev/CI
        // database may already have revenue from other paid orders, so this can't
        // assume it's the only (or highest) revenue in the view (no fixed rank/100% share).
        mockMvc.perform(get("/reports/product-sales").with(as(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.productId == %d)]".formatted(paidProduct.getId())).exists())
                .andExpect(jsonPath("$[?(@.productId == %d)].unitsSold".formatted(paidProduct.getId())).value(2))
                .andExpect(jsonPath("$[?(@.productId == %d)].revenue".formatted(paidProduct.getId())).value(91.80))
                .andExpect(jsonPath("$[?(@.productId == %d)].revenueRank".formatted(paidProduct.getId())).exists())
                .andExpect(jsonPath("$[?(@.productId == %d)]".formatted(unpaidProduct.getId())).doesNotExist());
    }
}
