package com.ericksoares.tattoo.user.application.service;

import com.ericksoares.tattoo.user.application.dto.request.UpdateUserRequest;
import com.ericksoares.tattoo.user.application.dto.response.UserResponse;
import com.ericksoares.tattoo.user.mapper.UserMapper;
import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.enums.UserStatus;
import com.ericksoares.tattoo.user.domain.enums.UserType;
import com.ericksoares.tattoo.user.domain.exception.UserNotFoundException;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateUserService {

    private final UserRepository repository;
    private final LastAdminGuard lastAdminGuard;

    public UserResponse execute(
            Long id,
            UpdateUserRequest request
    ) {

        User user = repository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException(id)
                );

        boolean willRemainActiveAdmin = request.userType() == UserType.ADMIN
                && request.userStatus() == UserStatus.ACTIVE;

        lastAdminGuard.ensureNotRemovingLastAdmin(user, willRemainActiveAdmin);

        user.setUsername(request.username());
        user.setFullName(request.fullName());
        user.setPhoneNumber(request.phoneNumber());
        user.setCpf(request.cpf());
        user.setImageUrl(request.imageUrl());
        user.setUserType(request.userType());
        user.setUserStatus(request.userStatus());

        repository.save(user);

        return UserMapper.toResponse(user);
    }
}
