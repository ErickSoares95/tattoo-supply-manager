package com.ericksoares.tattoo.shared.security.model;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Security principal. Holds plain values instead of the user module's JPA entity so that
 * shared never depends on user (the user module builds it, see CustomUserDetailsService).
 * {@code role} is the UserType name (ADMIN, CLIENT, ATTENDANT).
 */
public class AuthenticatedUser implements UserDetails {

    private final Long id;

    private final String email;

    private final String password;

    private final String role;

    private final boolean enabled;

    public AuthenticatedUser(Long id, String email, String password, String role, boolean enabled) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    @Override
    public Collection<? extends SimpleGrantedAuthority> getAuthorities() {

        return List.of(
                new SimpleGrantedAuthority(
                        "ROLE_" + role
                )
        );
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
