package com.ericksoares.tattoo.user.application.service;

import com.ericksoares.tattoo.user.application.dto.request.UpdateProfileRequest;
import com.ericksoares.tattoo.user.application.dto.response.UserResponse;
import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.exception.CpfAlreadyExistsException;
import com.ericksoares.tattoo.user.domain.exception.UserNotFoundException;
import com.ericksoares.tattoo.user.domain.exception.UsernameAlreadyExistsException;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;
import com.ericksoares.tattoo.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Self-service counterpart to {@link UpdateUserService} - callers pass their own id
 * (resolved from the JWT principal in the controller, never from a path variable), and
 * unlike the admin version, userType/userStatus are never touched here: there's no
 * LastAdminGuard check to run because a profile edit can't change either one (see
 * UpdateProfileRequest for why they're not even fields on this DTO).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateProfileService {

    private final UserRepository repository;

    public UserResponse execute(
            Long userId,
            UpdateProfileRequest request
    ) {

        User user = repository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(userId)
                );

        String normalizedCpf = UserMapper.normalizeCpf(request.cpf());

        validateUsername(request.username(), userId);
        validateCpf(normalizedCpf, userId);

        user.setUsername(request.username());
        user.setFullName(request.fullName());
        user.setPhoneNumber(request.phoneNumber());
        user.setCpf(normalizedCpf);
        user.setImageUrl(request.imageUrl());

        repository.save(user);

        return UserMapper.toResponse(user);
    }

    private void validateUsername(String username, Long userId) {

        if (repository.existsByUsernameAndIdNot(username, userId)) {
            throw new UsernameAlreadyExistsException(username);
        }
    }

    private void validateCpf(String cpf, Long userId) {

        if (cpf != null && repository.existsByCpfAndIdNot(cpf, userId)) {
            throw new CpfAlreadyExistsException(cpf);
        }
    }
}
