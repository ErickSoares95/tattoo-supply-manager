package com.ericksoares.tattoo.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
class ChangePasswordService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public void execute(Long userId, ChangePasswordRequest request) {

        User user = repository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidCurrentPasswordException();
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        // Logs out every session holding a token issued before now, this one included.
        user.setPasswordChangedAt(Instant.now());

        repository.save(user);
    }
}
