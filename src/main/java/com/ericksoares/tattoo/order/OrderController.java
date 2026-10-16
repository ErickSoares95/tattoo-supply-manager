package com.ericksoares.tattoo.order;

import com.ericksoares.tattoo.shared.security.AuthenticatedUser;
import com.ericksoares.tattoo.user.UserType;
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
class OrderController {

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
                .body(service.execute(request, authenticatedUser.getId()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<Page<OrderResponse>> findAll(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            Pageable pageable
    ) {
        boolean isAdmin = UserType.ADMIN.name().equals(authenticatedUser.getRole());

        return ResponseEntity.ok(
                findOrdersService.execute(authenticatedUser.getId(), isAdmin, pageable)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<OrderResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        boolean isAdmin = UserType.ADMIN.name().equals(authenticatedUser.getRole());

        return ResponseEntity.ok(
                findOrderByIdService.execute(id, authenticatedUser.getId(), isAdmin)
        );
    }
}
