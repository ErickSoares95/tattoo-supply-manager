package com.ericksoares.tattoo.user.application.mapper;

import com.ericksoares.tattoo.user.application.dto.UserResponse;
import com.ericksoares.tattoo.user.domain.entity.User;

public class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getUserStatus(),
                user.getUserType(),
                user.getPhoneNumber(),
                user.getImageUrl(),
                user.getCreationDate()
        );
    }
}
