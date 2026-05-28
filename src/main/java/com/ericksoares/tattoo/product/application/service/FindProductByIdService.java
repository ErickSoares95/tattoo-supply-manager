package com.ericksoares.tattoo.product.application.service;

import com.ericksoares.tattoo.product.application.dto.ProductResponse;
import com.ericksoares.tattoo.product.application.mapper.ProductMapper;
import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.domain.exception.ProductNotFoundException;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindProductByIdService {

    private final ProductRepository repository;

    public ProductResponse execute(Long id) {

        Product product = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return  ProductMapper.toResponse(product);
    }
}
