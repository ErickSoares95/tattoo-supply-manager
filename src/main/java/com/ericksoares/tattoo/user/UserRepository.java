package com.ericksoares.tattoo.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

interface UserRepository extends
        JpaRepository<User, Long>,
        JpaSpecificationExecutor<User>
{
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByCpf(String cpf);

    /**
     * SELECT ... FOR UPDATE over the matching rows, ordered by id so concurrent callers lock in
     * the same order (no deadlock). Used by LastAdminGuard: a count alone allows write skew.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<User> findByUserTypeAndUserStatusOrderById(UserType userType, UserStatus userStatus);
}