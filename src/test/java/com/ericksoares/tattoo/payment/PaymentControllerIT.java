package com.ericksoares.tattoo.payment;

import com.ericksoares.tattoo.order.OrderSummary;
import com.ericksoares.tattoo.order.OrderTestFixtures;
import com.ericksoares.tattoo.shared.security.AuthenticatedUser;
import com.ericksoares.tattoo.user.UserTestFixtures;
import com.ericksoares.tattoo.user.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** T1: POST/GET /orders/{orderId}/payments over HTTP (auth, ownership, rules, validation). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    private AuthenticatedUser owner;
    private OrderSummary order;

    @BeforeEach
    void createOrder() {
        owner = UserTestFixtures.createUser(context, "payer" + System.nanoTime() + "@test.com", UserType.CLIENT);
        // product_id has no FK (cross-module decoupling), so no product is needed here.
        order = OrderTestFixtures.saveOrder(context, owner.getId(), 999_998L, "Payment Ink", 2, new BigDecimal("25.00"));
    }

    private RequestPostProcessor as(AuthenticatedUser principal) {
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private String payment(String amount, String method) {
        return """
                {"amount":%s,"method":"%s"}
                """.formatted(amount, method);
    }

    private String paymentsUrl() {
        return "/orders/" + order.id() + "/payments";
    }

    @Test
    void ownerPaysTheExactTotalAndThePaymentIsApproved() throws Exception {
        mockMvc.perform(post(paymentsUrl()).with(as(owner)).contentType(MediaType.APPLICATION_JSON)
                        .content(payment("50.00", "CREDIT_CARD")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.method").value("CREDIT_CARD"));

        mockMvc.perform(get(paymentsUrl()).with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void aWrongAmountIsRecordedAsRejected() throws Exception {
        mockMvc.perform(post(paymentsUrl()).with(as(owner)).contentType(MediaType.APPLICATION_JSON)
                        .content(payment("10.00", "PIX")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void aSecondApprovedPaymentIsAConflict() throws Exception {
        mockMvc.perform(post(paymentsUrl()).with(as(owner)).contentType(MediaType.APPLICATION_JSON)
                        .content(payment("50.00", "PIX")))
                .andExpect(status().isCreated());

        mockMvc.perform(post(paymentsUrl()).with(as(owner)).contentType(MediaType.APPLICATION_JSON)
                        .content(payment("50.00", "PIX")))
                .andExpect(status().isConflict());
    }

    @Test
    void aCancelledOrderCannotBePaid() throws Exception {
        mockMvc.perform(post("/orders/" + order.id() + "/cancel").with(as(owner)))
                .andExpect(status().isOk());

        mockMvc.perform(post(paymentsUrl()).with(as(owner)).contentType(MediaType.APPLICATION_JSON)
                        .content(payment("50.00", "PIX")))
                .andExpect(status().isConflict());
    }

    @Test
    void anotherClientCanNeitherPayNorSeeThePayments() throws Exception {
        AuthenticatedUser intruder = UserTestFixtures.createUser(context, "intruder" + System.nanoTime() + "@test.com", UserType.CLIENT);

        mockMvc.perform(post(paymentsUrl()).with(as(intruder)).contentType(MediaType.APPLICATION_JSON)
                        .content(payment("50.00", "PIX")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(paymentsUrl()).with(as(intruder)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anUnknownMethodIsABadRequest() throws Exception {
        mockMvc.perform(post(paymentsUrl()).with(as(owner)).contentType(MediaType.APPLICATION_JSON)
                        .content(payment("50.00", "BITCOIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void withoutATokenItIsUnauthorized() throws Exception {
        mockMvc.perform(post(paymentsUrl()).contentType(MediaType.APPLICATION_JSON)
                        .content(payment("50.00", "PIX")))
                .andExpect(status().isUnauthorized());
    }
}
