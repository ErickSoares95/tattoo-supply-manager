package com.ericksoares.tattoo.user;

import java.time.LocalDateTime;

record UserResponse(
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
