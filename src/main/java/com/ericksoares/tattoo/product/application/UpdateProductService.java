package com.ericksoares.tattoo.product.application;

import com.ericksoares.tattoo.product.application.dto.request.UpdateProductRequest;
import com.ericksoares.tattoo.product.application.dto.response.ProductResponse;
import com.ericksoares.tattoo.product.application.mapper.ProductMapper;
import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.domain.exception.ProductNotFoundException;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateProductService {

    private final ProductRepository repository;

    public ProductResponse execute(
            Long id,
            UpdateProductRequest request
    ) {

        Product product = repository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(id)
                );

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());

        return ProductMapper.toResponse(product);
    }
}
