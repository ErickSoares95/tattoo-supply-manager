package com.ericksoares.tattoo.product.presentation.controller;

import com.ericksoares.tattoo.product.application.dto.request.ProductFilterRequest;
import com.ericksoares.tattoo.product.application.dto.request.ProductRequest;
import com.ericksoares.tattoo.product.application.dto.request.UpdateProductRequest;
import com.ericksoares.tattoo.product.application.dto.response.ProductResponse;
import com.ericksoares.tattoo.product.application.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class ProductController {

    private final RegisterProductService createProductService;
    private final FindAllProductsService findAllProductsService;
    private final FindProductByIdService findProductByIdService;
    private final UpdateProductService updateProductService;
    private final DeleteProductService deleteProductService;



    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createProductService.execute(request));
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

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @RequestBody UpdateProductRequest request
    ) {
        return ResponseEntity.ok(updateProductService.execute(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
         deleteProductService.execute(id);

         return ResponseEntity.noContent().build();
    }
}
