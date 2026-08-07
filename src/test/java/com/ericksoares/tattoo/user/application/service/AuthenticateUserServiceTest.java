package com.ericksoares.tattoo.user.application.service;

import com.ericksoares.tattoo.shared.security.JwtService;
import com.ericksoares.tattoo.shared.security.model.AuthenticatedUser;
import com.ericksoares.tattoo.user.application.dto.request.LoginRequest;
import com.ericksoares.tattoo.user.application.dto.response.LoginResponse;
import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.enums.UserStatus;
import com.ericksoares.tattoo.user.domain.enums.UserType;
import com.ericksoares.tattoo.user.domain.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticateUserService service;

    private User buildUser() {
        return User.builder()
                .id(1L)
                .email("admin@email.com")
                .password("encoded-password")
                .fullName("Admin")
                .userType(UserType.ADMIN)
                .userStatus(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void shouldAuthenticateSuccessfullyByEmail() {

        LoginRequest request = new LoginRequest("admin@email.com", "123456");

        User user = buildUser();

        Authentication mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getPrincipal())
                .thenReturn(new AuthenticatedUser(user));

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuthentication);

        when(jwtService.generateToken(user))
                .thenReturn("jwt-token");

        LoginResponse response = service.execute(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals("admin@email.com", response.email());
    }

    @Test
    void shouldAuthenticateSuccessfullyByCpf() {

        LoginRequest request = new LoginRequest("11122233344", "123456");

        User user = buildUser();

        Authentication mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getPrincipal())
                .thenReturn(new AuthenticatedUser(user));

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuthentication);

        when(jwtService.generateToken(user))
                .thenReturn("jwt-token");

        LoginResponse response = service.execute(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
    }

    @Test
    void shouldThrowWhenCredentialsAreInvalid() {

        LoginRequest request = new LoginRequest("wrong@email.com", "123456");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.execute(request)
        );
    }
}
