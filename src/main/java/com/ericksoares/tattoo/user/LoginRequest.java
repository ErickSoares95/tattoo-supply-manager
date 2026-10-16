package com.ericksoares.tattoo.user;

import jakarta.validation.constraints.NotBlank;

record LoginRequest(

        @NotBlank
        String login,

        @NotBlank
        String password
) {
}
