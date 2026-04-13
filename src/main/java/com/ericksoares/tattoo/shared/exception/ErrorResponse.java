package com.ericksoares.tattoo.shared.exception;

public record ErrorResponse(
        int status,
        String message
) {}
