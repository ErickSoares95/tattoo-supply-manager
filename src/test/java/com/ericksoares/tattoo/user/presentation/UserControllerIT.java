package com.ericksoares.tattoo.user.presentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateUserWithoutAuthentication()
            throws Exception {

        String json = """
                {
                  "username":"admin",
                  "email":"admin@tattoo.com",
                  "password":"123456",
                  "fullName":"Administrador",
                  "userType":"ADMIN"
                }
                """;

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(json)
                )
                .andExpect(
                        status().isCreated()
                );
    }
}
