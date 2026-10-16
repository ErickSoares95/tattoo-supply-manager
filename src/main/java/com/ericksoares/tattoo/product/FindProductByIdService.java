package com.ericksoares.tattoo.product;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class FindProductByIdService {

    private final ProductRepository repository;
    private final ProductSalesLookup salesLookup;

    public ProductResponse execute(Long id) {

        Product product = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        long unitsSold = salesLookup.unitsSoldFor(id, salesLookup.loadUnitsSoldByProductId());

        return ProductMapper.toResponse(product, unitsSold);
    }
}
