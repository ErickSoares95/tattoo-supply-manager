package com.ericksoares.tattoo.payment.application.dto;

import java.time.LocalDateTime;

public record KafkaPingMessage(
        String text,
        LocalDateTime sentAt
) {}
