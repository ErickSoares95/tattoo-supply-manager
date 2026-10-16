package com.ericksoares.tattoo.order;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends BaseException {

    public OrderNotFoundException(Long id) {
        super("Order not found with id: " + id, HttpStatus.NOT_FOUND);
    }
}
