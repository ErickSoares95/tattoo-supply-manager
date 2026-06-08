package com.ericksoares.tattoo.user.application.dto.request;

import com.ericksoares.tattoo.user.domain.enums.UserStatus;
import com.ericksoares.tattoo.user.domain.enums.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @NotBlank
        @Size(min = 3, max = 30)
        String username,

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
        UserType userType,

        @NotNull
        UserStatus userStatus

) {
}