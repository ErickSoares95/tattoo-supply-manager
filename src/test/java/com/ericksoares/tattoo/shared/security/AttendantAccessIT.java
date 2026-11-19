package com.ericksoares.tattoo.shared.security;

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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T2: the ATTENDANT role end to end - store staff who read reports and use the assistant, but
 * don't manage products, users, orders or notifications and don't shop.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AttendantAccessIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    private RequestPostProcessor attendant;

    @BeforeEach
    void createAttendant() {
        AuthenticatedUser principal = UserTestFixtures.createUser(
                context, "attendant" + System.nanoTime() + "@test.com", UserType.ATTENDANT);
        attendant = authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void readsTheSalesReport() throws Exception {
        mockMvc.perform(get("/reports/product-sales").with(attendant)).andExpect(status().isOk());
    }

    @Test
    void browsesTheCatalogAndOwnProfile() throws Exception {
        mockMvc.perform(get("/products").with(attendant)).andExpect(status().isOk());
        mockMvc.perform(get("/users/me").with(attendant)).andExpect(status().isOk());
    }

    @Test
    void cannotManageProducts() throws Exception {
        mockMvc.perform(post("/products").with(attendant).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"description\":\"Valid description\",\"price\":1.00,\"stock\":1,\"category\":\"INKS\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/products/1").with(attendant)).andExpect(status().isForbidden());
    }

    @Test
    void cannotManageUsers() throws Exception {
        mockMvc.perform(get("/users").with(attendant)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/users/1").with(attendant)).andExpect(status().isForbidden());
    }

    @Test
    void cannotPlaceOrSeeOrders() throws Exception {
        mockMvc.perform(get("/orders").with(attendant)).andExpect(status().isForbidden());
        mockMvc.perform(post("/orders").with(attendant).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1,\"quantity\":1}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotTouchNotifications() throws Exception {
        mockMvc.perform(get("/notifications/failed").with(attendant)).andExpect(status().isForbidden());
        mockMvc.perform(post("/notifications/reprocess").with(attendant)).andExpect(status().isForbidden());
    }
}
