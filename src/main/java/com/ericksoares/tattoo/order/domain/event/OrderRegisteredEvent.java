package com.ericksoares.tattoo.order.domain.event;

import java.math.BigDecimal;

public record OrderRegisteredEvent(
        BigDecimal total,
        int totalItems
) {}
