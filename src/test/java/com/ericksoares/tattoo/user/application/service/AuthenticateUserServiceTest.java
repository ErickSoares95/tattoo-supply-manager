package com.ericksoares.tattoo.user.application.service;

import com.ericksoares.tattoo.shared.security.JwtService;
import com.ericksoares.tattoo.user.application.dto.request.LoginRequest;
import com.ericksoares.tattoo.user.application.dto.response.LoginResponse;
import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.enums.UserStatus;
import com.ericksoares.tattoo.user.domain.enums.UserType;
import com.ericksoares.tattoo.user.domain.exception.InvalidCredentialsException;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticateUserService service;

    @Test
    void shouldAuthenticateSuccessfully() {

        LoginRequest request =
                new LoginRequest(
                        "admin@email.com",
                        "123456"
                );

        User user = User.builder()
                .email("admin@email.com")
                .password("encoded-password")
                .userType(UserType.ADMIN)
                .userStatus(UserStatus.ACTIVE)
                .build();

        when(repository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                request.password(),
                user.getPassword()
        )).thenReturn(true);

        when(jwtService.generateToken(user))
                .thenReturn("jwt-token");

        LoginResponse response =
                service.execute(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
    }

    @Test
    void shouldThrowWhenEmailNotFound() {

        LoginRequest request =
                new LoginRequest(
                        "wrong@email.com",
                        "123456"
                );

        when(repository.findByEmail(request.email()))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.execute(request)
        );
    }

    @Test
    void shouldThrowWhenPasswordIsInvalid() {

        LoginRequest request =
                new LoginRequest(
                        "admin@email.com",
                        "wrong"
                );

        User user = User.builder()
                .email("admin@email.com")
                .password("encoded")
                .build();

        when(repository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                request.password(),
                user.getPassword()
        )).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.execute(request)
        );
    }
}
