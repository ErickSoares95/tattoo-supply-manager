package com.ericksoares.tattoo.product.application.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
        String description,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than zero")
        @DecimalMin("0.01")
        BigDecimal price,

        @NotNull(message = "Stock is required")
        @Min(value = 0, message = "Stock cannot be negative")
        Integer stock,

        // Optional on purpose, same as User.imageUrl - admin pastes a URL from wherever
        // the image is already hosted, no upload/storage endpoint added for this.
        @Size(max = 255, message = "Image URL must be at most 255 characters")
        String imageUrl
) {}