package com.ericksoares.tattoo.shared.exception;

import java.util.Map;

record ErrorResponse(
        int status,
        String message,
        Map<String, String> errors
) {}
