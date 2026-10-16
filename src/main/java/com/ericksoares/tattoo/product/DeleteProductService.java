package com.ericksoares.tattoo.product;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
class DeleteProductService {

    private final ProductRepository repository;
    private final ApplicationEventPublisher publisher;

    public void execute(Long id) {

        Product product = repository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(id)
                );

        repository.delete(product);

        // Without it the ai module's vector index kept the deleted product and the assistant
        // could still recommend it.
        publisher.publishEvent(new ProductDeletedEvent(id));
    }
}
