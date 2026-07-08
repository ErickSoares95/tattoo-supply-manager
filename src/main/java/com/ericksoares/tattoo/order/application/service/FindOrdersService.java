package com.ericksoares.tattoo.order.application.service;

import com.ericksoares.tattoo.order.application.dto.OrderResponse;
import com.ericksoares.tattoo.order.application.mapper.OrderMapper;
import com.ericksoares.tattoo.order.domain.entity.Order;
import com.ericksoares.tattoo.order.infrasctruture.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindOrdersService {

    private final OrderRepository repository;

    public Page<OrderResponse> execute(Long userId, boolean isAdmin, Pageable pageable) {

        Page<Order> orders = isAdmin
                ? repository.findAll(pageable)
                : repository.findByUserId(userId, pageable);

        return orders.map(OrderMapper::toResponse);
    }
}
