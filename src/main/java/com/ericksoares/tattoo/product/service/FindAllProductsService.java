package com.ericksoares.tattoo.product.service;

import com.ericksoares.tattoo.product.dto.PageResponse;
import com.ericksoares.tattoo.product.dto.ProductResponse;
import com.ericksoares.tattoo.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class FindAllProductsService {

    private final ProductRepository repository;

    public FindAllProductsService(ProductRepository repository) {
        this.repository = repository;
    }

    public Page<ProductResponse> execute(Pageable pageable) {
        return repository.findAll(pageable)
                .map(product -> new ProductResponse(
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        product.getStock()
                ));
    }
}
