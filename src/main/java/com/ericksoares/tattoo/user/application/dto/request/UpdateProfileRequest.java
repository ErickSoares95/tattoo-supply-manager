package com.ericksoares.tattoo.user.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Self-service counterpart to {@link UpdateUserRequest}: same editable fields an admin
 * can touch, minus userType/userStatus - a client editing their own profile must never
 * be able to grant themselves a role or reactivate a blocked account, so those two stay
 * out of this DTO entirely rather than being validated-and-ignored in the service.
 */
public record UpdateProfileRequest(

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
        String imageUrl

) {
}
