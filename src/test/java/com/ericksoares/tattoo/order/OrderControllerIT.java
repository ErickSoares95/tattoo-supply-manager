package com.ericksoares.tattoo.order;

import com.ericksoares.tattoo.product.ProductSnapshot;
import com.ericksoares.tattoo.product.ProductTestFixtures;
import com.ericksoares.tattoo.shared.security.AuthenticatedUser;
import com.ericksoares.tattoo.user.UserTestFixtures;
import com.ericksoares.tattoo.user.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private OrderRepository orderRepository;

    private AuthenticatedUser createUser(String email, UserType type) {
        return UserTestFixtures.createUser(context, email, type);
    }

    private ProductSnapshot createProduct() {
        return ProductTestFixtures.createProduct(context, "Tinta Black", BigDecimal.TEN, 10);
    }

    private Order createOrder(Long userId, Long productId) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setProductName("Tinta Black");
        item.setQuantity(1);
        item.setPrice(BigDecimal.TEN);

        Order order = new Order();
        order.setUserId(userId);
        order.setItems(List.of(item));
        order.calculateTotal();

        return orderRepository.save(order);
    }

    private RequestPostProcessor as(AuthenticatedUser principal) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        return authentication(auth);
    }

    @Test
    void shouldReturnUnauthorizedWhenNoToken() throws Exception {

        mockMvc.perform(get("/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateOrderAsClient() throws Exception {

        AuthenticatedUser client = createUser("cliente1@teste.com", UserType.CLIENT);
        ProductSnapshot product = createProduct();

        String body = """
                {"items":[{"productId":%d,"quantity":2}]}
                """.formatted(product.id());

        mockMvc.perform(
                        post("/orders")
                                .with(as(client))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(client.getId()))
                .andExpect(jsonPath("$.total").value(20.0));
    }

    @Test
    void shouldRejectEmptyItemsWithBadRequest() throws Exception {

        AuthenticatedUser client = createUser("cliente2@teste.com", UserType.CLIENT);

        mockMvc.perform(
                        post("/orders")
                                .with(as(client))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"items\":[]}")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldOnlyListOwnOrdersAsClient() throws Exception {

        AuthenticatedUser client1 = createUser("cliente3@teste.com", UserType.CLIENT);
        AuthenticatedUser client2 = createUser("cliente4@teste.com", UserType.CLIENT);
        ProductSnapshot product = createProduct();

        createOrder(client1.getId(), product.id());
        createOrder(client2.getId(), product.id());

        mockMvc.perform(get("/orders").with(as(client1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(client1.getId()));
    }

    @Test
    void shouldDenyAccessToOtherClientsOrder() throws Exception {

        AuthenticatedUser owner = createUser("dono@teste.com", UserType.CLIENT);
        AuthenticatedUser other = createUser("outro@teste.com", UserType.CLIENT);
        ProductSnapshot product = createProduct();

        Order order = createOrder(owner.getId(), product.id());

        mockMvc.perform(get("/orders/" + order.getId()).with(as(other)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToAccessAnyOrder() throws Exception {

        AuthenticatedUser owner = createUser("dono2@teste.com", UserType.CLIENT);
        AuthenticatedUser admin = createUser("admin1@teste.com", UserType.ADMIN);
        ProductSnapshot product = createProduct();

        Order order = createOrder(owner.getId(), product.id());

        mockMvc.perform(get("/orders/" + order.getId()).with(as(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(owner.getId()));
    }
}
