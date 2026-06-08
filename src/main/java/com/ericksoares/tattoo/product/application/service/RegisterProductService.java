package com.ericksoares.tattoo.product.application.service;

import com.ericksoares.tattoo.product.application.dto.request.ProductRequest;
import com.ericksoares.tattoo.product.application.dto.response.ProductResponse;
import com.ericksoares.tattoo.product.application.mapper.ProductMapper;
import com.ericksoares.tattoo.product.domain.entity.Product;
import com.ericksoares.tattoo.product.domain.event.ProductRegisteredEvent;
import com.ericksoares.tattoo.product.infrastructure.repository.ProductRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterProductService {

    private final ProductRepository repository;
    private final ApplicationEventPublisher publisher;

    public RegisterProductService(ProductRepository repository, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Transactional
    public ProductResponse execute(ProductRequest request) {

        Product product = ProductMapper.toEntity(request);

        product.validate();

        Product saved = repository.save(product);

        publisher.publishEvent(
                new ProductRegisteredEvent(
                        saved.getId(),
                        saved.getName(),
                        saved.getPrice()
                )
        );

        return ProductMapper.toResponse(saved);
    }
}