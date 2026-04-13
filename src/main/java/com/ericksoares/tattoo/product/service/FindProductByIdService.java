package com.ericksoares.tattoo.product.service;

import com.ericksoares.tattoo.product.dto.ProductResponse;
import com.ericksoares.tattoo.product.entity.Product;
import com.ericksoares.tattoo.product.exception.ProductNotFoundException;
import com.ericksoares.tattoo.product.repository.ProductRepository;
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

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock()
        );
    }
}
