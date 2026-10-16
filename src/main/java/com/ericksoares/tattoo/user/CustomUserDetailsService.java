package com.ericksoares.tattoo.user;

import com.ericksoares.tattoo.shared.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lives in the user module (it owns the User entity) and is the only place that turns a
 * User into the security principal. shared/security only sees Spring's UserDetailsService
 * and the entity-free {@link AuthenticatedUser}, so shared never depends on user.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository repository;

    /**
     * "username" here is Spring Security's generic term for the login principal -
     * in this app it's whatever the client sent as LoginRequest.login(), which can
     * be either the user's email or their CPF (see AuthenticateUserService).
     */
    @Override
    public UserDetails loadUserByUsername(String login)
            throws UsernameNotFoundException {

        User user = repository.findByEmailOrCpf(login, login)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found"
                        ));

        return toAuthenticatedUser(user);
    }

    public static AuthenticatedUser toAuthenticatedUser(User user) {

        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getUserType().name(),
                user.getUserStatus() == UserStatus.ACTIVE
        );
    }
}
