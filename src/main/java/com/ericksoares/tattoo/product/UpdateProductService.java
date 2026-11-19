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

        // Orders decrease stock while the admin form is open; saving that form's stock
        // back would undo them. The @Version check on save only covers the window between
        // this read and the commit, so the version the client loaded is compared here.
        if (!product.getVersion().equals(request.version())) {
            throw new ProductModifiedException(id);
        }

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setImageUrl(request.imageUrl());
        product.setCategory(request.category());

        product.validate();

        // Flush so the response carries the incremented version: the form's next save sends
        // it back, and the pre-flush value would be refused as stale.
        repository.saveAndFlush(product);

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
