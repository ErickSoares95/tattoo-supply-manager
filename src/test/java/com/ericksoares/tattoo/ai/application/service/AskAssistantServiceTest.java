package com.ericksoares.tattoo.ai.application.service;

import com.ericksoares.tattoo.ai.application.dto.AskResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AskAssistantServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private LlmClient llmClient;

    @Test
    void shouldReturnCannedResponseWithoutCallingLlmWhenNothingIndexed() {

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of());

        AskAssistantService service = new AskAssistantService(vectorStore, llmClient);

        AskResponse response = service.execute("Do you have black ink?");

        assertTrue(response.sources().isEmpty());
        verifyNoInteractions(llmClient);
    }

    @Test
    void shouldGroundPromptOnRetrievedContextAndReturnSources() {

        Document document = Document.builder()
                .id("7")
                .text("Tattoo Ink - Black: Professional black tattoo ink")
                .metadata("productId", 7L)
                .metadata("productName", "Tattoo Ink - Black")
                .build();

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(document));

        when(llmClient.complete(anyString()))
                .thenReturn("Yes, we have Tattoo Ink - Black in stock.");

        AskAssistantService service = new AskAssistantService(vectorStore, llmClient);

        AskResponse response = service.execute("Do you have black ink?");

        assertEquals("Yes, we have Tattoo Ink - Black in stock.", response.answer());
        assertEquals(List.of("Tattoo Ink - Black"), response.sources());
    }
}
