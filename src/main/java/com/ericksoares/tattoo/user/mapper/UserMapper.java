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
                .cpf(normalizeCpf(request.cpf()))
                .imageUrl(request.imageUrl())
                .userStatus(UserStatus.ACTIVE)
                .userType(UserType.CLIENT)
                .build();
    }

    /**
     * Strips punctuation (dots, dashes) so "111.222.333-44" and "11122233344" are treated
     * as the same CPF for both storage and uniqueness checks - also fixes a latent bug where
     * a punctuated CPF (up to 14 chars, allowed by CreateUserRequest/UpdateUserRequest's
     * @Size) would overflow the "cpf varchar(11)" column and fail at the database level.
     */
    public static String normalizeCpf(String cpf) {

        if (cpf == null || cpf.isBlank()) {
            return null;
        }

        return cpf.replaceAll("[^0-9]", "");
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
                user.getCpf(),
                user.getImageUrl(),
                user.getCreationDate()
        );
    }
}
