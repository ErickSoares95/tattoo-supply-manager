package com.ericksoares.tattoo.user.application.service;

import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.enums.UserStatus;
import com.ericksoares.tattoo.user.domain.enums.UserType;
import com.ericksoares.tattoo.user.domain.exception.LastAdminException;
import com.ericksoares.tattoo.user.infrastructure.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LastAdminGuard {

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
