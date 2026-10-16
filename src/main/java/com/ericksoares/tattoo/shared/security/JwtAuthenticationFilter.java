package com.ericksoares.tattoo.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;

    // Implemented by the user module (CustomUserDetailsService); shared only knows the interface.
    private final UserDetailsService
            userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader =
                request.getHeader("Authorization");

        final String jwt;

        final String userEmail;

        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);

            return;
        }

        jwt = authHeader.substring(7);

        // A bad token (malformed, forged, expired) or a deleted user is not a server error: the
        // request just goes on unauthenticated, and a protected route answers 401 via the entry point.
        final Claims claims;
        final UserDetails userDetails;

        try {
            claims = jwtService.parse(jwt);
            userEmail = claims.getSubject();
            userDetails = userEmail == null ? null : userDetailsService.loadUserByUsername(userEmail);
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            log.debug("Ignoring invalid bearer token: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        if (userDetails != null
                && SecurityContextHolder
                .getContext()
                .getAuthentication() == null) {

            if (jwtService.isTokenValid(
                    claims,
                    userDetails
            )) {

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
