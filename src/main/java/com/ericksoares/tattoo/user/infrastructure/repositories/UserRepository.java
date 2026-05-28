package com.ericksoares.tattoo.user.infrastructure.repositories;


import com.ericksoares.tattoo.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends
        JpaRepository<User, Long>,
        JpaSpecificationExecutor<User>
{}