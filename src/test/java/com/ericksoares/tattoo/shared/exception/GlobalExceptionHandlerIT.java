package com.ericksoares.tattoo.shared.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Client mistakes raised by Spring MVC must come back as 4xx in the usual ErrorResponse shape,
 * not as the catch-all 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class GlobalExceptionHandlerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnNotFoundForUnknownRoute() throws Exception {

        mockMvc.perform(get("/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturnBadRequestForMalformedBody() throws Exception {

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{items:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void shouldReturnBadRequestForNonNumericId() throws Exception {

        mockMvc.perform(get("/orders/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    @Test
    void shouldReturnBadRequestForUnknownSortProperty() throws Exception {

        mockMvc.perform(get("/products").param("sort", "doesNotExist"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sort property 'doesNotExist'"));
    }

    @Test
    void shouldReturnConflictForDataIntegrityViolation() {

        var response = new GlobalExceptionHandler().handleDataIntegrity(
                new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertEquals(409, response.getStatusCode().value());
    }

    @Test
    void shouldReturnConflictForOptimisticLockFailure() {

        var response = new GlobalExceptionHandler().handleOptimisticLock(
                new OptimisticLockingFailureException("stale version"));

        assertEquals(409, response.getStatusCode().value());
    }

    @Test
    void shouldReturnMethodNotAllowedForUnsupportedMethod() throws Exception {

        mockMvc.perform(delete("/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }
}
