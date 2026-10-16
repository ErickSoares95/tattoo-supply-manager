package com.ericksoares.tattoo.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

record OrderRequest(
        @NotEmpty
        @Valid
        List<OrderItemRequest> items
) {}
