package com.ericksoares.tattoo.user.application.service;

import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.enums.UserStatus;
import com.ericksoares.tattoo.user.domain.enums.UserType;
import com.ericksoares.tattoo.user.domain.exception.LastAdminException;
import com.ericksoares.tattoo.user.domain.exception.UserNotFoundException;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteUserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private LastAdminGuard lastAdminGuard;

    @InjectMocks
    private DeleteUserService service;

    @Test
    void shouldDeleteUser() {

        User user = User.builder()
                .id(1L)
                .userType(UserType.CLIENT)
                .userStatus(UserStatus.ACTIVE)
                .build();

        when(repository.findById(1L))
                .thenReturn(Optional.of(user));

        service.execute(1L);

        verify(lastAdminGuard).ensureNotRemovingLastAdmin(user, false);
        verify(repository).delete(user);
    }

    @Test
    void shouldThrowWhenUserNotFound() {

        when(repository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> service.execute(1L)
        );

        verify(repository, never()).delete(any(User.class));
    }

    @Test
    void shouldThrowWhenDeletingTheLastActiveAdmin() {

        User user = User.builder()
                .id(1L)
                .username("admin")
                .userType(UserType.ADMIN)
                .userStatus(UserStatus.ACTIVE)
                .build();

        when(repository.findById(1L))
                .thenReturn(Optional.of(user));

        doThrow(new LastAdminException())
                .when(lastAdminGuard)
                .ensureNotRemovingLastAdmin(user, false);

        assertThrows(
                LastAdminException.class,
                () -> service.execute(1L)
        );

        verify(repository, never()).delete(any(User.class));
    }
}
