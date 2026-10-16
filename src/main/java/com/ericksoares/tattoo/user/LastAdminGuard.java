package com.ericksoares.tattoo.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class LastAdminGuard {

    private final UserRepository repository;

    public void ensureNotRemovingLastAdmin(User user, boolean willRemainActiveAdmin) {

        boolean wasActiveAdmin = user.getUserType() == UserType.ADMIN
                && user.getUserStatus() == UserStatus.ACTIVE;

        if (wasActiveAdmin && !willRemainActiveAdmin) {

            long activeAdmins = repository.countByUserTypeAndUserStatus(
                    UserType.ADMIN,
                    UserStatus.ACTIVE
            );

            if (activeAdmins <= 1) {
                throw new LastAdminException();
            }
        }
    }
}
