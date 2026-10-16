package com.ericksoares.tattoo.ai;

import jakarta.validation.constraints.NotBlank;

record AssistantPingRequest(

        @NotBlank(message = "Prompt is required")
        String prompt
) {}
