package com.ericksoares.tattoo.payment.presentation.controller;

import com.ericksoares.tattoo.payment.infrastructure.kafka.KafkaPingProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Temporary diagnostic endpoint for increment 0 of the payment/Kafka rollout.
 * Not part of the payment domain itself — just proves the broker is reachable
 * before {@code PaymentProcessedEvent} depends on it (increment 2).
 */
@RestController
@RequestMapping("/payments/diagnostics")
@CrossOrigin(origins = "*", maxAge = 3600)
public class KafkaDiagnosticsController {

    private final KafkaPingProducer producer;

    public KafkaDiagnosticsController(KafkaPingProducer producer) {
        this.producer = producer;
    }

    @PostMapping("/kafka-ping")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> kafkaPing() {

        producer.send("ping sent at " + LocalDateTime.now());

        return ResponseEntity.accepted().body(Map.of(
                "status", "sent",
                "note", "check application logs for consumer receipt (topic: " + KafkaPingProducer.TOPIC + ")"
        ));
    }
}
