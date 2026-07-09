package com.ericksoares.tattoo.user.application.dto.response;

import com.ericksoares.tattoo.user.domain.enums.UserStatus;
import com.ericksoares.tattoo.user.domain.enums.UserType;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        UserStatus userStatus,
        UserType userType,
        String phoneNumber,
        String cpf,
        String imageUrl,
        LocalDateTime creationDate
) {
}
