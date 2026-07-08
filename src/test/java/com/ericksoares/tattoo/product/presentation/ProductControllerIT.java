package com.ericksoares.tattoo.product.presentation;

import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldReturnUnauthorizedWhenNoToken() throws Exception {

        mockMvc.perform(
                        get("/products")
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    @WithMockUser(
            username = "admin",
            roles = {"ADMIN"}
    )
    void shouldAllowAdminFindProducts() throws Exception {

        mockMvc.perform(
                        get("/products")
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    @WithMockUser(
            username = "attendant",
            roles = {"ATTENDANT"}
    )
    void shouldAllowAttendantFindProducts() throws Exception {

        mockMvc.perform(
                        get("/products")
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    @WithMockUser(
            username = "attendant",
            roles = {"ATTENDANT"}
    )
    void shouldDenyAttendantCreateProduct() throws Exception {

        String request = """
            {
              "name":"Ink Black",
              "description":"Black tattoo ink",
              "price":99.90,
              "stock":10
            }
            """;

        mockMvc.perform(
                        post("/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldAllowAdminCreateProduct() throws Exception {

        String request = """
        {
          "name":"Ink Black",
          "description":"Black tattoo ink",
          "price":99.90,
          "stock":10
        }
        """;

        mockMvc.perform(
                        post("/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(
                        status().isCreated()
                );
    }

    @Test
    @WithMockUser(
            username = "attendant",
            roles = {"ATTENDANT"}
    )
    void shouldDenyAttendantDeleteProduct() throws Exception {

        mockMvc.perform(
                        delete("/products/1")
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    @WithMockUser(
            username = "admin",
            roles = {"ADMIN"}
    )
    void shouldAllowAdminDeleteProduct() throws Exception {

        Product product = Product.builder()
                .name("Agulha 0rlrl")
                .price(new BigDecimal("15.00"))
                .stock(50)
                .build();

        product = productRepository.save(product);


        mockMvc.perform(
                        delete("/products/" + product.getId())
                )
                .andExpect(
                        status().isNoContent()
                );
    }
}
