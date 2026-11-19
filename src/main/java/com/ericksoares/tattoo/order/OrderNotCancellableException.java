package com.ericksoares.tattoo.order;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

class OrderNotCancellableException extends BaseException {

    public OrderNotCancellableException(Long orderId, OrderStatus status) {
        super("Order " + orderId + " can't be cancelled: it is " + status, HttpStatus.CONFLICT);
    }
}
