package com.ericksoares.tattoo.product;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidStockQuantityException extends BaseException {

    public InvalidStockQuantityException() {
        super("Quantity must be greater than zero", HttpStatus.BAD_REQUEST);
    }
}
