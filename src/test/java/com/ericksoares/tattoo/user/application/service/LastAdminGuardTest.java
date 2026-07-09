package com.ericksoares.tattoo.user.application.service;

import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.enums.UserStatus;
import com.ericksoares.tattoo.user.domain.enums.UserType;
import com.ericksoares.tattoo.user.domain.exception.LastAdminException;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LastAdminGuardTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private LastAdminGuard guard;

    private User activeAdmin() {
        return User.builder()
                .id(1L)
                .userType(UserType.ADMIN)
                .userStatus(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void shouldThrowWhenDemotingTheOnlyActiveAdmin() {

        when(repository.countByUserTypeAndUserStatus(UserType.ADMIN, UserStatus.ACTIVE))
                .thenReturn(1L);

        assertThrows(
                LastAdminException.class,
                () -> guard.ensureNotRemovingLastAdmin(activeAdmin(), false)
        );
    }

    @Test
    void shouldAllowDemotingWhenAnotherActiveAdminExists() {

        when(repository.countByUserTypeAndUserStatus(UserType.ADMIN, UserStatus.ACTIVE))
                .thenReturn(2L);

        assertDoesNotThrow(
                () -> guard.ensureNotRemovingLastAdmin(activeAdmin(), false)
        );
    }

    @Test
    void shouldNotCheckWhenUserIsNotCurrentlyAnActiveAdmin() {

        User client = User.builder()
                .id(2L)
                .userType(UserType.CLIENT)
                .userStatus(UserStatus.ACTIVE)
                .build();

        assertDoesNotThrow(
                () -> guard.ensureNotRemovingLastAdmin(client, false)
        );
    }

    @Test
    void shouldNotThrowWhenUserWillRemainAnActiveAdmin() {

        assertDoesNotThrow(
                () -> guard.ensureNotRemovingLastAdmin(activeAdmin(), true)
        );
    }
}
