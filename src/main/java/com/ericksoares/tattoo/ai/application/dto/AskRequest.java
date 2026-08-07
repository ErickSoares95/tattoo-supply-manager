package com.ericksoares.tattoo.ai.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AskRequest(

        @NotBlank(message = "Question is required")
        String question
) {}
