package com.ericksoares.tattoo.user.application.dto.dto.request;

import com.ericksoares.tattoo.user.domain.enums.UserStatus;
import com.ericksoares.tattoo.user.domain.enums.UserType;

public record UserFilterRequest(
        String username,
        String email,
        UserStatus userStatus,
        UserType userType
) {
}
