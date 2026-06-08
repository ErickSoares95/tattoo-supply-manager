package com.ericksoares.tattoo.user.application.service;

import com.ericksoares.tattoo.user.application.dto.request.CreateUserRequest;
import com.ericksoares.tattoo.user.application.dto.response.UserResponse;
import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.enums.UserType;
import com.ericksoares.tattoo.user.domain.exception.EmailAlreadyExistsException;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;
import com.ericksoares.tattoo.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CreateUserService service;

    @Test
    void shouldCreateUser() {

        CreateUserRequest request =
                new CreateUserRequest(
                        "admin",
                        "admin@tattoo.com",
                        "123456",
                        "Administrador",
                        null,
                        null,
                        null,
                        UserType.ADMIN
                );

        User user =
                UserMapper.toEntity(request);

        when(repository.existsByEmail(any()))
                .thenReturn(false);

        when(repository.existsByUsername(any()))
                .thenReturn(false);

        when(passwordEncoder.encode(any()))
                .thenReturn("senha-criptografada");

        when(repository.save(any(User.class)))
                .thenReturn(user);

        UserResponse response =
                service.execute(request);

        assertNotNull(response);

        verify(repository)
                .save(any(User.class));
    }

    @Test
    void shouldThrowWhenEmailExists() {

        CreateUserRequest request =
                new CreateUserRequest(
                        "admin",
                        "admin@tattoo.com",
                        "123456",
                        "Administrador",
                        null,
                        null,
                        null,
                        UserType.ADMIN
                );

        when(repository.existsByEmail(any()))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> service.execute(request)
        );
    }
}
