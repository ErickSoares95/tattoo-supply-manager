package com.ericksoares.tattoo.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class LastAdminGuard {

    private final UserRepository repository;

    /**
     * Runs in the caller's transaction (MANDATORY) and locks every active admin row until it
     * commits. With a plain count, two admins demoting each other at the same time would both
     * see 2 and both succeed (write skew), leaving no admin. With the lock the second caller
     * waits; once the first commits, Postgres re-checks the WHERE clause, the demoted row drops
     * out, and the second caller sees 1 and is refused.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void ensureNotRemovingLastAdmin(User user, boolean willRemainActiveAdmin) {

        boolean wasActiveAdmin = user.getUserType() == UserType.ADMIN
                && user.getUserStatus() == UserStatus.ACTIVE;

        if (wasActiveAdmin && !willRemainActiveAdmin) {

            int activeAdmins = repository.findByUserTypeAndUserStatusOrderById(
                    UserType.ADMIN,
                    UserStatus.ACTIVE
            ).size();

            if (activeAdmins <= 1) {
                throw new LastAdminException();
            }
        }
    }
}
