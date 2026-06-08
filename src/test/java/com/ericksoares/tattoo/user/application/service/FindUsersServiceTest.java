package com.ericksoares.tattoo.user.application.service;

import com.ericksoares.tattoo.user.application.dto.request.UserFilterRequest;
import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class FindUsersServiceTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private FindUsersService service;

    @Test
    void shouldReturnPageOfUsers() {

        User user = User.builder()
                .id(1L)
                .username("erick")
                .build();

        Page<User> page =
                new PageImpl<>(List.of(user));

        UserFilterRequest filter =
                new UserFilterRequest(
                        null,
                        null,
                        null,
                        null
                );

        Pageable pageable =
                PageRequest.of(0, 10);

        when(repository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(page);

        var result =
                service.execute(
                        filter,
                        pageable
                );

        assertEquals(
                1,
                result.getTotalElements()
        );
    }
}
