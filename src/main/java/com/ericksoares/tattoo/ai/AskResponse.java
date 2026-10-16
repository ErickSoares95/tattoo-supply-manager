package com.ericksoares.tattoo.ai;

import java.util.List;

record AskResponse(
        String answer,
        List<String> sources
) {}
