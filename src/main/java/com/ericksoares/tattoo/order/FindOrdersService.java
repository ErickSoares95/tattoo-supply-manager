package com.ericksoares.tattoo.order;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class FindOrdersService {

    private final OrderRepository repository;

    public Page<OrderResponse> execute(Long userId, boolean isAdmin, Pageable pageable) {

        Page<Order> orders = isAdmin
                ? repository.findAll(pageable)
                : repository.findByUserId(userId, pageable);

        return orders.map(OrderMapper::toResponse);
    }
}
