package com.ericksoares.tattoo.product.domain.exception;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InsufficientStockException extends BaseException {

    public InsufficientStockException(String productName) {
        super("Insufficient stock for product: " + productName, HttpStatus.BAD_REQUEST);
    }
}
