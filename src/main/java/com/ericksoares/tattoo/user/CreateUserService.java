package com.ericksoares.tattoo.user;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
class CreateUserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher publisher;

    public UserResponse execute(CreateUserRequest request) {

        validateEmail(request.email());

        validateUsername(request.username());

        User user = UserMapper.toEntity(request);

        validateCpf(user.getCpf());

        user.setPassword(
                passwordEncoder.encode(request.password())
        );

        User savedUser = repository.save(user);

        publisher.publishEvent(
                new UserRegisteredEvent(
                        savedUser.getId(),
                        savedUser.getEmail(),
                        savedUser.getFullName(),
                        LocalDateTime.now()
                )
        );

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

    private void validateCpf(String cpf) {

        if (cpf != null && repository.existsByCpf(cpf)) {
            throw new CpfAlreadyExistsException(cpf);
        }
    }
}
