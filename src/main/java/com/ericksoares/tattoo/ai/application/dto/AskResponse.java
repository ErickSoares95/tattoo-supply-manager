package com.ericksoares.tattoo.ai.application.dto;

import java.util.List;

public record AskResponse(
        String answer,
        List<String> sources
) {}
