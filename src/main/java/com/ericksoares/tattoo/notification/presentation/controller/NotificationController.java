package com.ericksoares.tattoo.notification.presentation.controller;

import com.ericksoares.tattoo.notification.application.service.NotificationReprocessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationReprocessService service;

    public NotificationController(NotificationReprocessService service) {
        this.service = service;
    }

    @PostMapping("/reprocess")
    public ResponseEntity<Void> reprocess() {
        service.reprocessAll();
        return ResponseEntity.ok().build();
    }
}
