package com.ericksoares.tattoo.product.application.dto.request;

import com.ericksoares.tattoo.product.domain.enums.ProductCategory;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateProductRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        @NotNull
        @Positive
        @DecimalMin("0.01")
        BigDecimal price,

        @NotNull
        @Min(0)
        Integer stock,

        // Optional, same as ProductRequest/User.imageUrl - a PUT still has to send this
        // field (full replacement, not a patch), but null/blank is a valid value meaning
        // "no image".
        @Size(max = 255)
        String imageUrl,

        @NotNull(message = "Category is required")
        ProductCategory category

) {
}
