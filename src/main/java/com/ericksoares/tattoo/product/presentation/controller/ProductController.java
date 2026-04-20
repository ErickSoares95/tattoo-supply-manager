package com.ericksoares.tattoo.product.presentation.controller;

import com.ericksoares.tattoo.product.application.dto.ProductFilterRequest;
import com.ericksoares.tattoo.product.application.dto.ProductRequest;
import com.ericksoares.tattoo.product.application.dto.ProductResponse;
import com.ericksoares.tattoo.product.application.service.RegisterProductService;
import com.ericksoares.tattoo.product.application.service.FindAllProductsService;
import com.ericksoares.tattoo.product.application.service.FindProductByIdService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final RegisterProductService createProductService;

    private final FindAllProductsService findAllProductsService;
    private final FindProductByIdService findProductByIdService;

    public ProductController(RegisterProductService createProductService, FindAllProductsService findAllProductsService, FindProductByIdService findProductByIdService) {
        this.createProductService = createProductService;
        this.findAllProductsService = findAllProductsService;
        this.findProductByIdService = findProductByIdService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(createProductService.execute(request));
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> findAll(
            @ModelAttribute ProductFilterRequest filter,
            Pageable pageable
    ) {
        return ResponseEntity.ok(findAllProductsService.execute(filter, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(findProductByIdService.execute(id));
    }
}
