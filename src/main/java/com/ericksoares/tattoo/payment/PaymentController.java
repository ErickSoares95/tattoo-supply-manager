package com.ericksoares.tattoo.payment;

import com.ericksoares.tattoo.shared.security.AuthenticatedUser;
import com.ericksoares.tattoo.user.UserType;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders/{orderId}/payments")
class PaymentController {

    private final ProcessPaymentService processService;
    private final FindOrderPaymentsService findService;

    public PaymentController(ProcessPaymentService processService, FindOrderPaymentsService findService) {
        this.processService = processService;
        this.findService = findService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<PaymentResponse> pay(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        boolean isAdmin = UserType.ADMIN.name().equals(authenticatedUser.getRole());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                processService.execute(orderId, request, authenticatedUser.getId(), isAdmin)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<List<PaymentResponse>> list(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        boolean isAdmin = UserType.ADMIN.name().equals(authenticatedUser.getRole());

        return ResponseEntity.ok(
                findService.execute(orderId, authenticatedUser.getId(), isAdmin)
        );
    }
}
