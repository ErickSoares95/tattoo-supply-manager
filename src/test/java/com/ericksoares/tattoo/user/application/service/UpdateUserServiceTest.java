package com.ericksoares.tattoo.user.application.service;

import com.ericksoares.tattoo.user.application.dto.request.UpdateUserRequest;
import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.enums.UserStatus;
import com.ericksoares.tattoo.user.domain.enums.UserType;
import com.ericksoares.tattoo.user.domain.exception.CpfAlreadyExistsException;
import com.ericksoares.tattoo.user.domain.exception.LastAdminException;
import com.ericksoares.tattoo.user.domain.exception.UserNotFoundException;
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
class UpdateUserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private LastAdminGuard lastAdminGuard;

    @InjectMocks
    private UpdateUserService service;

    @Test
    void shouldUpdateUserSuccessfully() {

        User user = User.builder()
                .id(1L)
                .username("olduser")
                .build();

        UpdateUserRequest request =
                new UpdateUserRequest(
                        "newuser",
                        "Erick Soares",
                        "81999999999",
                        "12345678901",
                        "https://image.com",
                        UserType.ADMIN,
                        UserStatus.ACTIVE
                );

        when(repository.findById(1L))
                .thenReturn(Optional.of(user));

        service.execute(1L, request);

        verify(repository).save(user);
    }

    @Test
    void shouldThrowWhenUserNotFound() {

        when(repository.findById(1L))
                .thenReturn(Optional.empty());

        UpdateUserRequest request =
                new UpdateUserRequest(
                        "newuser",
                        "Erick Soares",
                        "81999999999",
                        "12345678901",
                        "https://image.com",
                        UserType.ADMIN,
                        UserStatus.ACTIVE
                );

        assertThrows(
                UserNotFoundException.class,
                () -> service.execute(1L, request)
        );
    }

    @Test
    void shouldThrowWhenLastAdminGuardRejectsTheChange() {

        User user = User.builder()
                .id(1L)
                .username("admin")
                .userType(UserType.ADMIN)
                .userStatus(UserStatus.ACTIVE)
                .build();

        UpdateUserRequest request =
                new UpdateUserRequest(
                        "admin",
                        "Admin",
                        null,
                        null,
                        null,
                        UserType.CLIENT,
                        UserStatus.ACTIVE
                );

        when(repository.findById(1L))
                .thenReturn(Optional.of(user));

        doThrow(new LastAdminException())
                .when(lastAdminGuard)
                .ensureNotRemovingLastAdmin(user, false);

        assertThrows(
                LastAdminException.class,
                () -> service.execute(1L, request)
        );

        verify(repository, never()).save(any());
    }

    @Test
    void shouldAllowUpdateWhenLastAdminGuardDoesNotReject() {

        User user = User.builder()
                .id(1L)
                .username("admin")
                .userType(UserType.ADMIN)
                .userStatus(UserStatus.ACTIVE)
                .build();

        UpdateUserRequest request =
                new UpdateUserRequest(
                        "admin",
                        "Admin",
                        null,
                        null,
                        null,
                        UserType.CLIENT,
                        UserStatus.ACTIVE
                );

        when(repository.findById(1L))
                .thenReturn(Optional.of(user));

        service.execute(1L, request);

        verify(lastAdminGuard).ensureNotRemovingLastAdmin(user, false);
        verify(repository).save(user);
    }

    @Test
    void shouldThrowWhenCpfAlreadyBelongsToAnotherUser() {

        User user = User.builder()
                .id(1L)
                .username("olduser")
                .build();

        UpdateUserRequest request =
                new UpdateUserRequest(
                        "newuser",
                        "Erick Soares",
                        "81999999999",
                        "111.222.333-44",
                        "https://image.com",
                        UserType.CLIENT,
                        UserStatus.ACTIVE
                );

        when(repository.findById(1L))
                .thenReturn(Optional.of(user));

        when(repository.existsByCpfAndIdNot("11122233344", 1L))
                .thenReturn(true);

        assertThrows(
                CpfAlreadyExistsException.class,
                () -> service.execute(1L, request)
        );

        verify(repository, never()).save(any());
    }

    @Test
    void shouldAllowUpdateWhenCpfBelongsToTheSameUser() {

        User user = User.builder()
                .id(1L)
                .username("olduser")
                .build();

        UpdateUserRequest request =
                new UpdateUserRequest(
                        "olduser",
                        "Erick Soares",
                        "81999999999",
                        "111.222.333-44",
                        "https://image.com",
                        UserType.CLIENT,
                        UserStatus.ACTIVE
                );

        when(repository.findById(1L))
                .thenReturn(Optional.of(user));

        when(repository.existsByCpfAndIdNot("11122233344", 1L))
                .thenReturn(false);

        service.execute(1L, request);

        assertEquals("11122233344", user.getCpf());

        verify(repository).save(user);
    }
}
