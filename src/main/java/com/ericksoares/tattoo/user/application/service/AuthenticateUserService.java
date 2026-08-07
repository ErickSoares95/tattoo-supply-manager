package com.ericksoares.tattoo.user.application.service;

import com.ericksoares.tattoo.shared.security.JwtService;
import com.ericksoares.tattoo.shared.security.model.AuthenticatedUser;
import com.ericksoares.tattoo.user.application.dto.request.LoginRequest;
import com.ericksoares.tattoo.user.application.dto.response.LoginResponse;
import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthenticateUserService {

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    public LoginResponse execute(LoginRequest request) {

        Authentication authentication;

        try {

            authentication = authenticationManager.authenticate(

                    new UsernamePasswordAuthenticationToken(
                            request.login(),
                            request.password()
                    )
            );

        } catch (BadCredentialsException ex) {

            throw new InvalidCredentialsException();
        }

        // CustomUserDetailsService already resolved login (email OR cpf) to this User -
        // reusing it here avoids a second, redundant repository lookup by a different key.
        User user = ((AuthenticatedUser) authentication.getPrincipal()).getUser();

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getUserType()
        );
    }
}
