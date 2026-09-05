package com.ericksoares.tattoo.payment.presentation.controller;

import com.ericksoares.tattoo.payment.application.dto.PaymentRequest;
import com.ericksoares.tattoo.payment.application.dto.PaymentResponse;
import com.ericksoares.tattoo.payment.application.service.FindOrderPaymentsService;
import com.ericksoares.tattoo.payment.application.service.ProcessPaymentService;
import com.ericksoares.tattoo.shared.security.model.AuthenticatedUser;
import com.ericksoares.tattoo.user.domain.enums.UserType;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders/{orderId}/payments")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PaymentController {

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
        boolean isAdmin = authenticatedUser.getUser().getUserType() == UserType.ADMIN;

        return ResponseEntity.status(HttpStatus.CREATED).body(
                processService.execute(orderId, request, authenticatedUser.getUser().getId(), isAdmin)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<List<PaymentResponse>> list(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        boolean isAdmin = authenticatedUser.getUser().getUserType() == UserType.ADMIN;

        return ResponseEntity.ok(
                findService.execute(orderId, authenticatedUser.getUser().getId(), isAdmin)
        );
    }
}
