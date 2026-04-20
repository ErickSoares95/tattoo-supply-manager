package com.ericksoares.tattoo.product.domain.exception;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidProductNameException extends BaseException {

    public InvalidProductNameException() {
        super("Product name is invalid", HttpStatus.BAD_REQUEST);
    }
}