package com.ericksoares.tattoo.shared.security;

import com.ericksoares.tattoo.shared.security.model.AuthenticatedUser;
import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.enums.UserStatus;
import com.ericksoares.tattoo.user.domain.enums.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    private User user;

    @BeforeEach
    void setup() {

        jwtService = new JwtService(
                "test-secret-key-apenas-para-testes-unitarios-32chars+",
                1000 * 60 * 60 * 24
        );

        user = User.builder()
                .email("admin@tattoo.com")
                .password("123456")
                .fullName("Admin")
                .userStatus(UserStatus.ACTIVE)
                .userType(UserType.ADMIN)
                .build();

        user.setId(1L);
    }

    @Test
    void shouldGenerateToken() {

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void shouldExtractUsername() {

        String token = jwtService.generateToken(user);

        String username =
                jwtService.extractUsername(token);

        assertEquals(
                "admin@tattoo.com",
                username
        );
    }

    @Test
    void shouldValidateToken() {

        String token =
                jwtService.generateToken(user);

        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(user);

        assertTrue(
                jwtService.isTokenValid(
                        token,
                        authenticatedUser
                )
        );
    }
}