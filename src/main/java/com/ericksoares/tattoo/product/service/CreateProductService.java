package com.ericksoares.tattoo.product.service;

import com.ericksoares.tattoo.product.dto.ProductRequest;
import com.ericksoares.tattoo.product.dto.ProductResponse;
import com.ericksoares.tattoo.product.entity.Product;
import com.ericksoares.tattoo.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class CreateProductService {
    private final ProductRepository repository;

    public CreateProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public ProductResponse execute(ProductRequest request) {

        Product product = new Product();
        product.setName(request.name());
        product.setPrice(request.price());
        product.setStock(request.stock());

        Product saved = repository.save(product);

        return new ProductResponse(
                saved.getId(),
                saved.getName(),
                saved.getPrice(),
                saved.getStock()
        );
    }
}
