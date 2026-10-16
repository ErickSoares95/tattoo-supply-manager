package com.ericksoares.tattoo.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
class DeleteUserService {

    private final UserRepository repository;
    private final LastAdminGuard lastAdminGuard;

    public void execute(Long id) {

        User user = repository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException(id)
                );

        lastAdminGuard.ensureNotRemovingLastAdmin(user, false);

        repository.delete(user);
    }
}
