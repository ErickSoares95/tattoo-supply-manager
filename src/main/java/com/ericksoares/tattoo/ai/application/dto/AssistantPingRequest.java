package com.ericksoares.tattoo.ai.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AssistantPingRequest(

        @NotBlank(message = "Prompt is required")
        String prompt
) {}
