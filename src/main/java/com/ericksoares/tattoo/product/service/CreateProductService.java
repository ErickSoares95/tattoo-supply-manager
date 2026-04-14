package com.ericksoares.tattoo.product.service;

import com.ericksoares.tattoo.product.dto.ProductRequest;
import com.ericksoares.tattoo.product.dto.ProductResponse;
import com.ericksoares.tattoo.product.entity.Product;
import com.ericksoares.tattoo.product.mapper.ProductMapper;
import com.ericksoares.tattoo.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class CreateProductService {
    private final ProductRepository repository;

    public CreateProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public ProductResponse execute(ProductRequest request) {

        Product product = ProductMapper.toEntity(request);

        Product saved = repository.save(product);

        return ProductMapper.toResponse(saved);
    }
}
