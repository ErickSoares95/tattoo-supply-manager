package com.ericksoares.tattoo.product;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RegisterProductService {

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