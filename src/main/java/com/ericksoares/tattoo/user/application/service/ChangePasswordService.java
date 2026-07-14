package com.ericksoares.tattoo.user.application.service;

import com.ericksoares.tattoo.user.application.dto.request.ChangePasswordRequest;
import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.exception.InvalidCurrentPasswordException;
import com.ericksoares.tattoo.user.domain.exception.UserNotFoundException;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChangePasswordService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public void execute(Long userId, ChangePasswordRequest request) {

        User user = repository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidCurrentPasswordException();
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));

        repository.save(user);
    }
}
