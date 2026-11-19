package com.ericksoares.tattoo.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // U7: intentional table-level read of the order module's orders table (same idea as
    // report's VIEW): order depends on user, so calling order's code from here would create a
    // user <-> order cycle.
    @Query(value = "SELECT EXISTS (SELECT 1 FROM orders WHERE user_id = :userId)", nativeQuery = true)
    boolean hasOrders(@Param("userId") Long userId);

    /**
     * SELECT ... FOR UPDATE over the matching rows, ordered by id so concurrent callers lock in
     * the same order (no deadlock). Used by LastAdminGuard: a count alone allows write skew.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<User> findByUserTypeAndUserStatusOrderById(UserType userType, UserStatus userStatus);
}