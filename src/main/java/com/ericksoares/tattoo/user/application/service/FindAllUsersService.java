package com.ericksoares.tattoo.user.application.service;

import com.ericksoares.tattoo.user.application.dto.request.UserFilterRequest;
import com.ericksoares.tattoo.user.application.dto.response.UserResponse;
import com.ericksoares.tattoo.user.mapper.UserMapper;
import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.infrastructure.specification.UserSpecification;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class FindAllUsersService {
    private final UserRepository repository;

    public FindAllUsersService(UserRepository repository) {
        this.repository = repository;
    }

    public Page<UserResponse> execute(
            UserFilterRequest filter,
            Pageable pageable
    ) {

        Specification<User> specification = Specification
                .where(UserSpecification.usernameContains(filter.username()))
                .and(UserSpecification.emailContains(filter.email()))
                .and(UserSpecification.hasStatus(filter.userStatus()))
                .and(UserSpecification.hasType(filter.userType()));

        return repository
                .findAll(specification, pageable)
                .map(UserMapper::toResponse);
    }
}
