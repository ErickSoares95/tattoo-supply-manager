package com.ericksoares.tattoo.user.service;

import com.ericksoares.tattoo.shared.security.JwtService;
import com.ericksoares.tattoo.user.application.dto.dto.request.LoginRequest;
import com.ericksoares.tattoo.user.application.dto.dto.response.LoginResponse;
import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.exception.InvalidCredentialsException;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthenticateUserService {

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final UserRepository repository;

    public LoginResponse execute(LoginRequest request) {

        try {

            authenticationManager.authenticate(

                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );

        } catch (BadCredentialsException ex) {

            throw new InvalidCredentialsException();
        }

        User user = repository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

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
