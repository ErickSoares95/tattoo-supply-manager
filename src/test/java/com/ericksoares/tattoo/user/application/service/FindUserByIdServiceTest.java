package com.ericksoares.tattoo.user.application.service;

import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.exception.UserNotFoundException;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindUserByIdServiceTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private FindUserByIdService service;

    @Test
    void shouldFindUserById() {

        User user = User.builder()
                .id(1L)
                .username("erick")
                .build();

        when(repository.findById(1L))
                .thenReturn(Optional.of(user));

        var response = service.execute(1L);

        assertNotNull(response);
        assertEquals("erick", response.username());
    }

    @Test
    void shouldThrowWhenUserNotFound() {

        when(repository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> service.execute(1L)
        );
    }
}
