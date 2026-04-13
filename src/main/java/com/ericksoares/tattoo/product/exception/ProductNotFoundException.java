package com.ericksoares.tattoo.product.exception;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends BaseException {

    public ProductNotFoundException(Long id) {
        super("Product not found with id: " + id, HttpStatus.NOT_FOUND);
    }
}