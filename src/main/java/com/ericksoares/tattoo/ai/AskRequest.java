package com.ericksoares.tattoo.ai;

import jakarta.validation.constraints.NotBlank;

record AskRequest(

        @NotBlank(message = "Question is required")
        String question
) {}
