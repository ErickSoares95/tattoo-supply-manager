package com.ericksoares.tattoo.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record AskRequest(

        // A2: bounded - every character goes into the LLM prompt (cost, latency, room to smuggle
        // instructions in). 500 is plenty for a product question.
        @NotBlank(message = "Question is required")
        @Size(max = 500, message = "Question must be at most 500 characters")
        String question
) {}
