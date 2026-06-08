package com.ericksoares.tattoo.user.presentation.controller;

import com.ericksoares.tattoo.user.application.dto.request.LoginRequest;
import com.ericksoares.tattoo.user.application.dto.response.LoginResponse;
import com.ericksoares.tattoo.user.application.service.AuthenticateUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticateUserService authenticateUserService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        return ResponseEntity.ok(
                authenticateUserService.execute(request)
        );
    }
}
