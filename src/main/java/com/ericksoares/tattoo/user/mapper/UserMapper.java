package com.ericksoares.tattoo.user.mapper;

import com.ericksoares.tattoo.user.application.dto.request.CreateUserRequest;
import com.ericksoares.tattoo.user.application.dto.response.UserResponse;
import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.enums.UserStatus;
import com.ericksoares.tattoo.user.domain.enums.UserType;

public class UserMapper {

    private UserMapper() {}

    public static User toEntity(CreateUserRequest request) {

        return User.builder()
                .username(request.username())
                .email(request.email())
                .password(request.password())
                .fullName(request.fullName())
                .phoneNumber(request.phoneNumber())
                .cpf(request.cpf())
                .imageUrl(request.imageUrl())
                .userStatus(UserStatus.ACTIVE)
                .userType(UserType.CLIENT)
                .build();
    }

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
