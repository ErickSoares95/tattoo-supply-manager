package com.ericksoares.tattoo.user.infrastructure.repositories;


import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.enums.UserStatus;
import com.ericksoares.tattoo.user.domain.enums.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserRepository extends
        JpaRepository<User, Long>,
        JpaSpecificationExecutor<User>
{
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailOrCpf(String email, String cpf);

    long countByUserTypeAndUserStatus(UserType userType, UserStatus userStatus);
}