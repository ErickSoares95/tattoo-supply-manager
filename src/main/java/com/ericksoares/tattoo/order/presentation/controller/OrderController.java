package com.ericksoares.tattoo.order.presentation.controller;

import com.ericksoares.tattoo.order.application.dto.OrderRequest;
import com.ericksoares.tattoo.order.application.dto.OrderResponse;
import com.ericksoares.tattoo.order.application.service.FindOrderByIdService;
import com.ericksoares.tattoo.order.application.service.FindOrdersService;
import com.ericksoares.tattoo.order.application.service.RegisterOrderService;
import com.ericksoares.tattoo.shared.security.model.AuthenticatedUser;
import com.ericksoares.tattoo.user.domain.enums.UserType;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*", maxAge = 3600)
public class OrderController {

    private final RegisterOrderService service;
    private final FindOrdersService findOrdersService;
    private final FindOrderByIdService findOrderByIdService;

    public OrderController(
            RegisterOrderService service,
            FindOrdersService findOrdersService,
            FindOrderByIdService findOrderByIdService
    ) {
        this.service = service;
        this.findOrdersService = findOrdersService;
        this.findOrderByIdService = findOrderByIdService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.execute(request, authenticatedUser.getUser().getId()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<Page<OrderResponse>> findAll(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            Pageable pageable
    ) {
        boolean isAdmin = authenticatedUser.getUser().getUserType() == UserType.ADMIN;

        return ResponseEntity.ok(
                findOrdersService.execute(authenticatedUser.getUser().getId(), isAdmin, pageable)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<OrderResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        boolean isAdmin = authenticatedUser.getUser().getUserType() == UserType.ADMIN;

        return ResponseEntity.ok(
                findOrderByIdService.execute(id, authenticatedUser.getUser().getId(), isAdmin)
        );
    }
}
