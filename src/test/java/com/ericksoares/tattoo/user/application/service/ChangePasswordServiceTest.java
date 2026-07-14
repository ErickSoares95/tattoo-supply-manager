package com.ericksoares.tattoo.user.application.service;

import com.ericksoares.tattoo.user.application.dto.request.ChangePasswordRequest;
import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.exception.InvalidCurrentPasswordException;
import com.ericksoares.tattoo.user.domain.exception.UserNotFoundException;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ChangePasswordService service;

    @Test
    void shouldChangePasswordWhenCurrentPasswordMatches() {

        User user = User.builder()
                .id(1L)
                .password("encoded-old-password")
                .build();

        ChangePasswordRequest request =
                new ChangePasswordRequest("oldPass123", "newPass456");

        when(repository.findById(1L))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("oldPass123", "encoded-old-password"))
                .thenReturn(true);

        when(passwordEncoder.encode("newPass456"))
                .thenReturn("encoded-new-password");

        service.execute(1L, request);

        assertEquals("encoded-new-password", user.getPassword());
        verify(repository).save(user);
    }

    @Test
    void shouldThrowWhenCurrentPasswordDoesNotMatch() {

        User user = User.builder()
                .id(1L)
                .password("encoded-old-password")
                .build();

        ChangePasswordRequest request =
                new ChangePasswordRequest("wrongPass", "newPass456");

        when(repository.findById(1L))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("wrongPass", "encoded-old-password"))
                .thenReturn(false);

        assertThrows(
                InvalidCurrentPasswordException.class,
                () -> service.execute(1L, request)
        );

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUserNotFound() {

        ChangePasswordRequest request =
                new ChangePasswordRequest("oldPass123", "newPass456");

        when(repository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> service.execute(1L, request)
        );

        verify(repository, never()).save(any());
    }
}
