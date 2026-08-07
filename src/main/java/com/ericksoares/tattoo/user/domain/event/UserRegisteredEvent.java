package com.ericksoares.tattoo.user.domain.event;

import java.time.LocalDateTime;

public record UserRegisteredEvent(
        Long userId,
        String email,
        String fullName,
        LocalDateTime occurredAt
) {}
