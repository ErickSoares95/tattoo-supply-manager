package com.ericksoares.tattoo.order.domain.event;

import com.ericksoares.tattoo.order.application.dto.OrderItemData;

import java.time.LocalDateTime;
import java.util.List;

public record OrderRegisteredEvent(
        Long orderId,
        List<OrderItemData> items,
        LocalDateTime occurredAt
) {}
