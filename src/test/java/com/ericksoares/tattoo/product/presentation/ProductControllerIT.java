package com.ericksoares.tattoo.product.presentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIT {

    @Autowired
    private MockMvc mockMvc;

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
            username = "employee",
            roles = {"EMPLOYEE"}
    )
    void shouldAllowEmployeeFindProducts() throws Exception {

        mockMvc.perform(
                        get("/products")
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    @WithMockUser(
            username = "employee",
            roles = {"EMPLOYEE"}
    )
    void shouldDenyEmployeeCreateProduct() throws Exception {

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
    @WithMockUser(
            username = "admin",
            roles = {"ADMIN"}
    )
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
            username = "employee",
            roles = {"EMPLOYEE"}
    )
    void shouldDenyEmployeeDeleteProduct() throws Exception {

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

        mockMvc.perform(
                        delete("/products/1")
                )
                .andExpect(
                        status().isNoContent()
                );
    }
}
