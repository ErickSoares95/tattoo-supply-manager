package com.ericksoares.tattoo.user.application.dto.request;

import com.ericksoares.tattoo.user.domain.enums.UserType;
import jakarta.validation.constraints.*;

public record CreateUserRequest(

        @NotBlank
        @Size(min = 3, max = 30)
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "Username must contain only letters, numbers, dots, underscores and hyphens"
        )
        String username,

        @Email
        @NotBlank
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Password must contain at least one letter and one number"
        )
        String password,

        @NotBlank
        @Size(min = 3, max = 150)
        String fullName,

        @Size(max = 20)
        String phoneNumber,

        @Size(min = 11, max = 14)
        String cpf,

        @Size(max = 255)
        String imageUrl,

        @NotNull
        UserType userType
) {
}
