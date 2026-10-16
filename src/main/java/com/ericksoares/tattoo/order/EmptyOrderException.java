package com.ericksoares.tattoo.order;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

class EmptyOrderException extends BaseException {

    public EmptyOrderException() {
        super("Order must have at least one item", HttpStatus.BAD_REQUEST);
    }
}
