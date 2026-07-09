package com.ericksoares.tattoo.order.presentation;

import com.ericksoares.tattoo.order.domain.entity.Order;
import com.ericksoares.tattoo.order.domain.entity.OrderItem;
import com.ericksoares.tattoo.order.infrasctruture.repository.OrderRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private User createUser(String email, UserType type) {
        User user = User.builder()
                .username(email.substring(0, email.indexOf('@')))
                .email(email)
                .password(passwordEncoder.encode("Senha1234"))
                .fullName("Usuario Teste")
                .userStatus(UserStatus.ACTIVE)
                .userType(type)
                .build();

        return userRepository.save(user);
    }

    private Product createProduct() {
        return productRepository.save(
                Product.builder()
                        .name("Tinta Black")
                        .price(BigDecimal.TEN)
                        .stock(10)
                        .build()
        );
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

    private RequestPostProcessor as(User user) {
        AuthenticatedUser principal = new AuthenticatedUser(user);
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

        User client = createUser("cliente1@teste.com", UserType.CLIENT);
        Product product = createProduct();

        String body = """
                {"items":[{"productId":%d,"quantity":2}]}
                """.formatted(product.getId());

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

        User client = createUser("cliente2@teste.com", UserType.CLIENT);

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

        User client1 = createUser("cliente3@teste.com", UserType.CLIENT);
        User client2 = createUser("cliente4@teste.com", UserType.CLIENT);
        Product product = createProduct();

        createOrder(client1.getId(), product.getId());
        createOrder(client2.getId(), product.getId());

        mockMvc.perform(get("/orders").with(as(client1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(client1.getId()));
    }

    @Test
    void shouldDenyAccessToOtherClientsOrder() throws Exception {

        User owner = createUser("dono@teste.com", UserType.CLIENT);
        User other = createUser("outro@teste.com", UserType.CLIENT);
        Product product = createProduct();

        Order order = createOrder(owner.getId(), product.getId());

        mockMvc.perform(get("/orders/" + order.getId()).with(as(other)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToAccessAnyOrder() throws Exception {

        User owner = createUser("dono2@teste.com", UserType.CLIENT);
        User admin = createUser("admin1@teste.com", UserType.ADMIN);
        Product product = createProduct();

        Order order = createOrder(owner.getId(), product.getId());

        mockMvc.perform(get("/orders/" + order.getId()).with(as(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(owner.getId()));
    }
}
