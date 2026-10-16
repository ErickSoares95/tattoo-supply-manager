package com.ericksoares.tattoo.user;

import com.ericksoares.tattoo.shared.security.AuthenticatedUser;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Test-only entry point for other modules' integration tests: User and UserRepository are
 * package-private, so tests outside the user module create users through here and only get
 * back the security principal.
 */
public final class UserTestFixtures {

    private UserTestFixtures() {}

    /** Persists an ACTIVE user (password "Senha1234") and returns its principal. */
    public static AuthenticatedUser createUser(ApplicationContext context, String email, UserType type) {
        PasswordEncoder passwordEncoder = context.getBean(PasswordEncoder.class);
        User user = User.builder()
                .username(email.substring(0, email.indexOf('@')))
                .email(email)
                .password(passwordEncoder.encode("Senha1234"))
                .fullName("Usuario Teste")
                .userStatus(UserStatus.ACTIVE)
                .userType(type)
                .build();

        return CustomUserDetailsService.toAuthenticatedUser(context.getBean(UserRepository.class).save(user));
    }
}
