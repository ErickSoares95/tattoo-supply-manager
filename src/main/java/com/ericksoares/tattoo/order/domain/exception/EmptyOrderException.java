package com.ericksoares.tattoo.order.domain.exception;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class EmptyOrderException extends BaseException {

    public EmptyOrderException() {
        super("Order must have at least one item", HttpStatus.BAD_REQUEST);
    }
}
