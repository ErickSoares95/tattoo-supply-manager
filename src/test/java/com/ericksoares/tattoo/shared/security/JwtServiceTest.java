package com.ericksoares.tattoo.shared.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    private AuthenticatedUser user;

    @BeforeEach
    void setup() {

        jwtService = new JwtService(
                "test-secret-key-apenas-para-testes-unitarios-32chars+",
                1000 * 60 * 60 * 24
        );

        user = new AuthenticatedUser(1L, "admin@tattoo.com", "123456", "ADMIN", true);
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

        assertTrue(
                jwtService.isTokenValid(
                        token,
                        user
                )
        );
    }

    @Test
    void shouldRejectExpiredToken() {

        JwtService shortLived = new JwtService(
                "test-secret-key-apenas-para-testes-unitarios-32chars+",
                -1000
        );

        String token = shortLived.generateToken(user);

        assertThrows(ExpiredJwtException.class, () -> jwtService.parse(token));
    }

    @Test
    void shouldRejectTokenSignedWithAnotherKey() {

        JwtService otherKey = new JwtService(
                "another-secret-key-for-a-forged-token-32chars++",
                1000 * 60
        );

        String forged = otherKey.generateToken(user);

        assertThrows(JwtException.class, () -> jwtService.parse(forged));
    }

    @Test
    void shouldNotValidateTokenWhenUserIsBlocked() {

        String token =
                jwtService.generateToken(user);

        AuthenticatedUser blockedUser =
                new AuthenticatedUser(1L, "admin@tattoo.com", "123456", "ADMIN", false);

        assertFalse(
                jwtService.isTokenValid(
                        token,
                        blockedUser
                )
        );
    }
}
