package com.ericksoares.tattoo.user.application.service;

import com.ericksoares.tattoo.shared.security.JwtService;
import com.ericksoares.tattoo.shared.security.model.AuthenticatedUser;
import com.ericksoares.tattoo.user.application.dto.request.LoginRequest;
import com.ericksoares.tattoo.user.application.dto.response.LoginResponse;
import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.exception.InvalidCredentialsException;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;
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

    private final UserRepository userRepository;

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

        // CustomUserDetailsService already resolved login (email OR cpf) to this principal -
        // reloading by its id (same transaction, so the persistence context already holds
        // the User) gets the profile fields the principal deliberately doesn't carry.
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(InvalidCredentialsException::new);

        String token = jwtService.generateToken(principal);

        return new LoginResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getUserType(),
                user.getImageUrl()
        );
    }
}
