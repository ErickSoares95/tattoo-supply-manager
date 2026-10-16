package com.ericksoares.tattoo.order;

import java.time.LocalDateTime;
import java.util.List;

public record OrderRegisteredEvent(
        Long orderId,
        List<OrderItemData> items,
        LocalDateTime occurredAt
) {}
