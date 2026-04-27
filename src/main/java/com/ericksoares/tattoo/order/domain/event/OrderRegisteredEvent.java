package com.ericksoares.tattoo.order.domain.event;

import java.time.LocalDateTime;

public record OrderRegisteredEvent(
        Long orderId,
        LocalDateTime occurredAt
) {}
