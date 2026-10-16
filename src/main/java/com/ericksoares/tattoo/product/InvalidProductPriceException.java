package com.ericksoares.tattoo.product;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

class InvalidProductPriceException extends BaseException {

    public InvalidProductPriceException() {
        super("Product price must be greater than zero", HttpStatus.BAD_REQUEST);
    }
}