package com.ericksoares.tattoo.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

record OrderItemRequest(
        @NotNull
        Long productId,

        @NotNull
        @Positive
        Integer quantity
) {}
