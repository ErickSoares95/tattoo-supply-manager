package com.ericksoares.tattoo.ai.presentation.controller;

import com.ericksoares.tattoo.ai.application.dto.AskRequest;
import com.ericksoares.tattoo.ai.application.dto.AskResponse;
import com.ericksoares.tattoo.ai.application.dto.AssistantPingRequest;
import com.ericksoares.tattoo.ai.application.dto.AssistantPingResponse;
import com.ericksoares.tattoo.ai.application.dto.RestockRecommendationResponse;
import com.ericksoares.tattoo.ai.application.service.AskAssistantService;
import com.ericksoares.tattoo.ai.application.service.IndexProductCatalogService;
import com.ericksoares.tattoo.ai.application.service.LlmClient;
import com.ericksoares.tattoo.ai.application.service.RecommendRestockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final LlmClient llmClient;
    private final RecommendRestockService recommendRestockService;
    private final AskAssistantService askAssistantService;
    private final IndexProductCatalogService indexProductCatalogService;

    @PostMapping("/ping")
    @PreAuthorize("hasAnyRole('ADMIN','ATTENDANT')")
    public ResponseEntity<AssistantPingResponse> ping(
            @Valid @RequestBody AssistantPingRequest request
    ) {

        String reply = llmClient.complete(request.prompt());

        return ResponseEntity.ok(new AssistantPingResponse(reply));
    }

    @GetMapping("/restock-recommendations")
    @PreAuthorize("hasAnyRole('ADMIN','ATTENDANT')")
    public ResponseEntity<RestockRecommendationResponse> restockRecommendations() {

        return ResponseEntity.ok(recommendRestockService.execute());
    }

    @PostMapping("/ask")
    @PreAuthorize("hasAnyRole('ADMIN','ATTENDANT')")
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request) {

        return ResponseEntity.ok(askAssistantService.execute(request.question()));
    }

    @PostMapping("/reindex-catalog")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> reindexCatalog() {

        int indexed = indexProductCatalogService.indexAll();

        return ResponseEntity.ok(Map.of("indexed", indexed));
    }
}
