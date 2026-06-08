package com.ericksoares.tattoo.user.service;

import com.ericksoares.tattoo.user.application.dto.dto.request.CreateUserRequest;
import com.ericksoares.tattoo.user.application.dto.dto.response.UserResponse;
import com.ericksoares.tattoo.user.mapper.UserMapper;
import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.exception.EmailAlreadyExistsException;
import com.ericksoares.tattoo.user.domain.exception.UsernameAlreadyExistsException;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateUserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse execute(CreateUserRequest request) {

        validateEmail(request.email());

        validateUsername(request.username());

        User user = UserMapper.toEntity(request);

        user.setPassword(
                passwordEncoder.encode(request.password())
        );

        User savedUser = repository.save(user);

        return UserMapper.toResponse(savedUser);
    }

    private void validateEmail(String email) {

        if (repository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
    }

    private void validateUsername(String username) {

        if (repository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }
    }
}
