package com.ericksoares.tattoo.user.application.service;

import com.ericksoares.tattoo.user.application.dto.response.UserResponse;
import com.ericksoares.tattoo.user.mapper.UserMapper;
import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.exception.UserNotFoundException;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindUserByIdService {

    private final UserRepository repository;

    public UserResponse execute(Long id) {

        User user = repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        return UserMapper.toResponse(user);
    }
}
