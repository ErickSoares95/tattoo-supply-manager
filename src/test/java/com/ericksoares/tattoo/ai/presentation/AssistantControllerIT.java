package com.ericksoares.tattoo.ai.presentation;

import com.ericksoares.tattoo.ai.application.dto.AskResponse;
import com.ericksoares.tattoo.ai.application.dto.RestockRecommendationResponse;
import com.ericksoares.tattoo.ai.application.service.AskAssistantService;
import com.ericksoares.tattoo.ai.application.service.IndexProductCatalogService;
import com.ericksoares.tattoo.ai.application.service.LlmClient;
import com.ericksoares.tattoo.ai.application.service.RecommendRestockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AssistantControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LlmClient llmClient;

    @MockBean
    private RecommendRestockService recommendRestockService;

    @MockBean
    private AskAssistantService askAssistantService;

    @MockBean
    private IndexProductCatalogService indexProductCatalogService;

    @Test
    void shouldReturnUnauthorizedWhenNoToken() throws Exception {

        mockMvc.perform(
                        post("/assistant/ping")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"prompt\":\"hello\"}")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "client", roles = {"CLIENT"})
    void shouldReturnForbiddenForClientRole() throws Exception {

        mockMvc.perform(
                        post("/assistant/ping")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"prompt\":\"hello\"}")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldReturnLlmReplyForAdmin() throws Exception {

        when(llmClient.complete(anyString()))
                .thenReturn("mocked reply");

        mockMvc.perform(
                        post("/assistant/ping")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"prompt\":\"hello\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("mocked reply"));
    }

    @Test
    @WithMockUser(username = "attendant", roles = {"ATTENDANT"})
    void shouldReturnBadRequestWhenPromptIsBlank() throws Exception {

        mockMvc.perform(
                        post("/assistant/ping")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"prompt\":\"\"}")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnauthorizedForRestockRecommendationsWhenNoToken() throws Exception {

        mockMvc.perform(get("/assistant/restock-recommendations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldReturnRestockRecommendationsForAdmin() throws Exception {

        when(recommendRestockService.execute())
                .thenReturn(new RestockRecommendationResponse("Restock ink soon.", List.of()));

        mockMvc.perform(get("/assistant/restock-recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendation").value("Restock ink soon."));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldReturnAskResponseForAdmin() throws Exception {

        when(askAssistantService.execute(anyString()))
                .thenReturn(new AskResponse("Yes, we have it.", List.of("Tattoo Ink - Black")));

        mockMvc.perform(
                        post("/assistant/ask")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"question\":\"Do you have black ink?\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Yes, we have it."));
    }

    @Test
    @WithMockUser(username = "attendant", roles = {"ATTENDANT"})
    void shouldReturnForbiddenForReindexCatalogWhenNotAdmin() throws Exception {

        mockMvc.perform(post("/assistant/reindex-catalog"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldReindexCatalogForAdmin() throws Exception {

        when(indexProductCatalogService.indexAll())
                .thenReturn(5);

        mockMvc.perform(post("/assistant/reindex-catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indexed").value(5));
    }
}
