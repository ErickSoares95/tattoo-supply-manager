package com.ericksoares.tattoo.user;

import com.ericksoares.tattoo.shared.security.JwtService;
import com.ericksoares.tattoo.shared.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

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
                .thenReturn(CustomUserDetailsService.toAuthenticatedUser(user));

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuthentication);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(jwtService.generateToken(any(AuthenticatedUser.class)))
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
                .thenReturn(CustomUserDetailsService.toAuthenticatedUser(user));

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuthentication);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(jwtService.generateToken(any(AuthenticatedUser.class)))
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
