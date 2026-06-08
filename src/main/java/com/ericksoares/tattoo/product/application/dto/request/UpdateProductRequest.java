package com.ericksoares.tattoo.product.application.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateProductRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        @NotNull
        @DecimalMin("0.0")
        BigDecimal price,

        @NotNull
        @Min(0)
        Integer stock

) {
}
