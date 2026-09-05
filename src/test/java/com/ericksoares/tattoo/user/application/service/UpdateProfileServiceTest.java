package com.ericksoares.tattoo.user.application.service;

import com.ericksoares.tattoo.user.application.dto.request.UpdateProfileRequest;
import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.enums.UserStatus;
import com.ericksoares.tattoo.user.domain.enums.UserType;
import com.ericksoares.tattoo.user.domain.exception.CpfAlreadyExistsException;
import com.ericksoares.tattoo.user.domain.exception.UserNotFoundException;
import com.ericksoares.tattoo.user.domain.exception.UsernameAlreadyExistsException;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateProfileServiceTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UpdateProfileService service;

    @Test
    void shouldUpdateOwnProfileSuccessfully() {

        User user = User.builder()
                .id(1L)
                .username("olduser")
                .userType(UserType.CLIENT)
                .userStatus(UserStatus.ACTIVE)
                .build();

        UpdateProfileRequest request =
                new UpdateProfileRequest(
                        "newuser",
                        "Erick Soares",
                        "81999999999",
                        "111.222.333-44",
                        "https://image.com"
                );

        when(repository.findById(1L))
                .thenReturn(Optional.of(user));

        service.execute(1L, request);

        assertEquals("newuser", user.getUsername());
        assertEquals("11122233344", user.getCpf());
        // The two admin-only fields must never move through this path.
        assertEquals(UserType.CLIENT, user.getUserType());
        assertEquals(UserStatus.ACTIVE, user.getUserStatus());

        verify(repository).save(user);
    }

    @Test
    void shouldThrowWhenUserNotFound() {

        when(repository.findById(1L))
                .thenReturn(Optional.empty());

        UpdateProfileRequest request =
                new UpdateProfileRequest("newuser", "Erick Soares", null, null, null);

        assertThrows(
                UserNotFoundException.class,
                () -> service.execute(1L, request)
        );
    }

    @Test
    void shouldThrowWhenUsernameAlreadyBelongsToAnotherUser() {

        User user = User.builder().id(1L).username("olduser").build();

        UpdateProfileRequest request =
                new UpdateProfileRequest("taken", "Erick Soares", null, null, null);

        when(repository.findById(1L))
                .thenReturn(Optional.of(user));

        when(repository.existsByUsernameAndIdNot("taken", 1L))
                .thenReturn(true);

        assertThrows(
                UsernameAlreadyExistsException.class,
                () -> service.execute(1L, request)
        );

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCpfAlreadyBelongsToAnotherUser() {

        User user = User.builder().id(1L).username("olduser").build();

        UpdateProfileRequest request =
                new UpdateProfileRequest("olduser", "Erick Soares", null, "111.222.333-44", null);

        when(repository.findById(1L))
                .thenReturn(Optional.of(user));

        when(repository.existsByUsernameAndIdNot("olduser", 1L))
                .thenReturn(false);

        when(repository.existsByCpfAndIdNot("11122233344", 1L))
                .thenReturn(true);

        assertThrows(
                CpfAlreadyExistsException.class,
                () -> service.execute(1L, request)
        );

        verify(repository, never()).save(any());
    }
}
