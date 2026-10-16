package com.ericksoares.tattoo.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
class UpdateUserService {

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

        String normalizedCpf = UserMapper.normalizeCpf(request.cpf());

        validateCpf(normalizedCpf, id);

        user.setUsername(request.username());
        user.setFullName(request.fullName());
        user.setPhoneNumber(request.phoneNumber());
        user.setCpf(normalizedCpf);
        user.setImageUrl(request.imageUrl());
        user.setUserType(request.userType());
        user.setUserStatus(request.userStatus());

        repository.save(user);

        return UserMapper.toResponse(user);
    }

    private void validateCpf(String cpf, Long userId) {

        if (cpf != null && repository.existsByCpfAndIdNot(cpf, userId)) {
            throw new CpfAlreadyExistsException(cpf);
        }
    }
}
