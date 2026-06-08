package com.ericksoares.tattoo.user.service;

import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.exception.UserNotFoundException;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteUserService {

    private final UserRepository repository;

    public void execute(Long id) {

        User user = repository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException(id)
                );

        repository.delete(user);
    }
}
