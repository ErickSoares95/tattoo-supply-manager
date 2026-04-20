package com.ericksoares.tattoo.product.domain.exception;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidStockException extends BaseException {

    public InvalidStockException() {
        super("Stock cannot be negative", HttpStatus.BAD_REQUEST);
    }
}
