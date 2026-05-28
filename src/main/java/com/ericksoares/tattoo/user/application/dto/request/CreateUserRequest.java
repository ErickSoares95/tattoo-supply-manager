package com.ericksoares.tattoo.user.application.dto.request;

import com.ericksoares.tattoo.user.domain.enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(

        @NotBlank
        String username,

        @Email
        @NotBlank
        String email,

        @NotBlank
        String password,

        @NotBlank
        String fullName,

        String phoneNumber,

        String cpf,

        String imageUrl,

        @NotNull
        UserType userType
) {
}
