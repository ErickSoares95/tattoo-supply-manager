package com.ericksoares.tattoo.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
class NotificationController {

    private final NotificationReprocessService service;
    private final PaymentNotificationReprocessService paymentService;
    private final FindFailedNotificationsService findFailedService;

    public NotificationController(
            NotificationReprocessService service,
            PaymentNotificationReprocessService paymentService,
            FindFailedNotificationsService findFailedService
    ) {
        this.service = service;
        this.paymentService = paymentService;
        this.findFailedService = findFailedService;
    }

    /** N11: unprocessed failures, newest first (max 200); {@code parked=true} = only the terminal ones. */
    @GetMapping("/failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FailedNotificationView>> failed(
            @RequestParam(defaultValue = "false") boolean parked
    ) {
        return ResponseEntity.ok(findFailedService.execute(parked));
    }

    @PostMapping("/reprocess")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reprocess() {
        service.reprocessAll();
        paymentService.reprocessAll();
        return ResponseEntity.ok().build();
    }
}
