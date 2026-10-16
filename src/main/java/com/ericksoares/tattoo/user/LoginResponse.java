package com.ericksoares.tattoo.user;

record LoginResponse(

        String token,

        Long userId,

        String fullName,

        String email,

        UserType userType,

        String imageUrl
) {
}
