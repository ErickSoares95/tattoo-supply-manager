package com.ericksoares.tattoo.order.application.dto;

import java.util.List;

public record OrderRequest(
        List<OrderItemRequest> items
) {}
