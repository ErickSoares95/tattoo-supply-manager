package com.ericksoares.tattoo.order;

import java.time.LocalDateTime;
import java.util.List;

/** Published after an order is cancelled and its stock given back (ai re-indexes those products). */
public record OrderCancelledEvent(
        Long orderId,
        List<OrderItemData> items,
        boolean expired,
        LocalDateTime occurredAt
) {}
