package com.ericksoares.tattoo.product;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    void shouldAllowAnonymousToBrowseCatalog() throws Exception {

        // GET /products and GET /products/{id} are public on purpose (storefront
        // redesign) - anonymous visitors can browse; mutations still require ADMIN.
        mockMvc.perform(
                        get("/products")
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    void shouldReturnUnauthorizedForNonGetRequestsWithoutToken() throws Exception {

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

        // Needs a fully valid body (category included) - otherwise @Valid rejects it
        // with 400 during argument resolution, before @PreAuthorize's proxy even runs,
        // and this test would be asserting the wrong thing (validation, not authorization).
        String request = """
            {
              "name":"Ink Black",
              "description":"Black tattoo ink",
              "price":99.90,
              "stock":10,
              "category":"INKS"
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
          "stock":10,
          "category":"INKS"
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

    @Test
    @WithMockUser(
            username = "admin",
            roles = {"ADMIN"}
    )
    void shouldValidateUpdateRequest() throws Exception {

        Product product = productRepository.save(Product.builder()
                .name("Tinta Azul")
                .price(new BigDecimal("30.00"))
                .stock(5)
                .build());

        // No category and a name longer than the column: rejected by Bean Validation (400)
        // instead of saving a null category or failing in the database (500).
        String request = """
            {
              "name":"%s",
              "description":"Blue tattoo ink",
              "price":30.00,
              "stock":5
            }
            """.formatted("x".repeat(150));

        mockMvc.perform(
                        put("/products/" + product.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.errors.name").exists()
                )
                .andExpect(
                        jsonPath("$.errors.category").exists()
                );
    }

    @Test
    void shouldFilterByDescription() throws Exception {

        productRepository.save(Product.builder()
                .name("Filtro Teste")
                .description("descricao-unica-para-o-filtro")
                .price(new BigDecimal("10.00"))
                .stock(1)
                .build());

        mockMvc.perform(
                        get("/products").param("description", "descricao-unica-para-o-filtro")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.totalElements").value(1)
                );
    }
}
