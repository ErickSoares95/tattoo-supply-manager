package com.ericksoares.tattoo.user;

record UserFilterRequest(
        String username,
        String email,
        UserStatus userStatus,
        UserType userType
) {
}
