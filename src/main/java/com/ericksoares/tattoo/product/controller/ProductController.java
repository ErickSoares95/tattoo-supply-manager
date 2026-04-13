package com.ericksoares.tattoo.product.controller;

import com.ericksoares.tattoo.product.dto.ProductRequest;
import com.ericksoares.tattoo.product.dto.ProductResponse;
import com.ericksoares.tattoo.product.entity.Product;
import com.ericksoares.tattoo.product.service.CreateProductService;
import com.ericksoares.tattoo.product.service.FindAllProductsService;
import com.ericksoares.tattoo.product.service.FindProductByIdService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final CreateProductService createProductService;

    private final FindAllProductsService findAllProductsService;
    private final FindProductByIdService findProductByIdService;

    public ProductController(CreateProductService createProductService, FindAllProductsService findAllProductsService, FindProductByIdService findProductByIdService) {
        this.createProductService = createProductService;
        this.findAllProductsService = findAllProductsService;
        this.findProductByIdService = findProductByIdService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestBody ProductRequest request) {
        return ResponseEntity.ok(createProductService.execute(request));
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> findAll(Pageable pageable) {
        return ResponseEntity.ok(findAllProductsService.execute(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(findProductByIdService.execute(id));
    }
}
