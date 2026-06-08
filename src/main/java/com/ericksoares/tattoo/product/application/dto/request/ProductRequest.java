package com.ericksoares.tattoo.product.application.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank
        @Min(value = (3), message = "Description cannot be shorter than 10")
        @Max(500)
        String description,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than zero")
        @DecimalMin("0.01")
        BigDecimal price,

        @NotNull(message = "Stock is required")
        @Min(value = 0, message = "Stock cannot be negative")
        Integer stock
) {}