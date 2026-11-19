package com.ericksoares.tattoo.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    @Query("select o.id from Order o where o.status = :status and o.creationDate < :before")
    List<Long> findIdsByStatusAndCreationDateBefore(@Param("status") OrderStatus status,
                                                    @Param("before") LocalDateTime before);

    Optional<Order> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
}
