package com.ericksoares.tattoo.product;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

record UpdateProductRequest(

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
        ProductCategory category,

        // The version the client loaded (ProductResponse.version). A full replacement of a
        // stale copy would overwrite stock sold since then, so a mismatch is a 409.
        @NotNull(message = "Version is required")
        Long version

) {
}
