package com.ericksoares.tattoo.product.application.service;

import com.ericksoares.tattoo.product.application.dto.ProductResponse;
import com.ericksoares.tattoo.product.application.mapper.ProductMapper;
import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.domain.exception.ProductNotFoundException;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class FindProductByIdService {

    private final ProductRepository repository;

    public FindProductByIdService(ProductRepository repository) {
        this.repository = repository;
    }

    public ProductResponse execute(Long id) {

        Product product = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return  ProductMapper.toResponse(product);
    }
}
