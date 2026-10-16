package com.ericksoares.tattoo.user;

import java.time.LocalDateTime;

public record UserRegisteredEvent(
        Long userId,
        String email,
        String fullName,
        LocalDateTime occurredAt
) {}
