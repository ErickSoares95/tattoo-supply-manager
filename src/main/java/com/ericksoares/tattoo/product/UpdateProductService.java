package com.ericksoares.tattoo.product;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
class UpdateProductService {

    private final ProductRepository repository;
    private final ApplicationEventPublisher publisher;

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
        product.setImageUrl(request.imageUrl());
        product.setCategory(request.category());

        product.validate();

        repository.save(product);

        publisher.publishEvent(
                new ProductUpdatedEvent(
                        product.getId(),
                        product.getName(),
                        product.getPrice()
                )
        );

        return ProductMapper.toResponse(product);
    }
}
