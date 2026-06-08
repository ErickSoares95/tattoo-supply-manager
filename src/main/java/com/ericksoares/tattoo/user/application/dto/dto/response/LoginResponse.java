package com.ericksoares.tattoo.user.application.dto.dto.response;

import com.ericksoares.tattoo.user.domain.enums.UserType;

public record LoginResponse(

        String token,

        Long userId,

        String fullName,

        String email,

        UserType userType
) {
}
