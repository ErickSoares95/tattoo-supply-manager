package com.ericksoares.tattoo.order.domain.exception;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidOrderItemQuantityException extends BaseException {

    public InvalidOrderItemQuantityException() {
        super("Quantity must be greater than zero", HttpStatus.BAD_REQUEST);
    }
}
