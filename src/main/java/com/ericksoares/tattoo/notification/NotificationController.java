package com.ericksoares.tattoo.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@CrossOrigin(origins = "*", maxAge = 3600)
class NotificationController {

    private final NotificationReprocessService service;
    private final PaymentNotificationReprocessService paymentService;

    public NotificationController(
            NotificationReprocessService service,
            PaymentNotificationReprocessService paymentService
    ) {
        this.service = service;
        this.paymentService = paymentService;
    }

    @PostMapping("/reprocess")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reprocess() {
        service.reprocessAll();
        paymentService.reprocessAll();
        return ResponseEntity.ok().build();
    }
}
