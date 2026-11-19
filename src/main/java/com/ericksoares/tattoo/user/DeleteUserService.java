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

        // U7: deleting would leave orders pointing at a user that no longer exists (no FK
        // between modules, by design). Blocking the account keeps the history and stops logins.
        if (repository.hasOrders(id)) {
            throw new UserHasOrdersException(id);
        }

        repository.delete(user);
    }
}
