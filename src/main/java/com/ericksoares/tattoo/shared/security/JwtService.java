package com.ericksoares.tattoo.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signKey;

    private final long expiration;

    public JwtService(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.expiration}") long expiration
    ) {
        // Explicit charset: the key bytes must not depend on the JVM's default encoding.
        this.signKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(AuthenticatedUser user) {

        Date now = new Date();

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(signKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifies the signature and the expiration and returns the claims. Throws
     * {@link JwtException} for a malformed, forged or expired token.
     */
    public Claims parse(String token) {

        return Jwts.parser()
                .verifyWith(signKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    /** Expiration is already enforced by {@link #parse}; this checks the user behind the token. */
    public boolean isTokenValid(Claims claims, UserDetails userDetails) {

        return claims.getSubject().equals(userDetails.getUsername())
                && userDetails.isEnabled();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return isTokenValid(parse(token), userDetails);
    }
}
