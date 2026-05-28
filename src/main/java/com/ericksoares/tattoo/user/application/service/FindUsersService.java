package com.ericksoares.tattoo.user.application.service;

import com.ericksoares.tattoo.user.application.dto.request.UserFilterRequest;
import com.ericksoares.tattoo.user.application.dto.response.UserResponse;
import com.ericksoares.tattoo.user.application.mapper.UserMapper;
import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;
import com.ericksoares.tattoo.user.infrastructure.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindUsersService {

    private final UserRepository repository;

    public Page<UserResponse> execute(
            UserFilterRequest filter,
            Pageable pageable
    ) {

        Specification<User> spec = Specification.where(
                UserSpecification.usernameContains(filter.username())
        ).and(
                UserSpecification.emailContains(filter.email())
        ).and(
                UserSpecification.hasStatus(filter.userStatus())
        ).and(
                UserSpecification.hasType(filter.userType())
        );

        return repository.findAll(spec, pageable)
                .map(UserMapper::toResponse);
    }
}
