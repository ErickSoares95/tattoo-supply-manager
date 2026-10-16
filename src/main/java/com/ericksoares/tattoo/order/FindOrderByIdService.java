package com.ericksoares.tattoo.order;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class FindOrderByIdService {

    private final OrderRepository repository;

    public OrderResponse execute(Long id, Long userId, boolean isAdmin) {

        Order order = repository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (!isAdmin && !order.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to access this order");
        }

        return OrderMapper.toResponse(order);
    }
}
