package com.ericksoares.tattoo.order.presentation.controller;

import com.ericksoares.tattoo.order.application.dto.OrderRequest;
import com.ericksoares.tattoo.order.application.service.RegisterOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*", maxAge = 3600)
public class OrderController {

    private final RegisterOrderService service;

    public OrderController(RegisterOrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody OrderRequest request) {
        return ResponseEntity.ok(service.execute(request));
    }
}
