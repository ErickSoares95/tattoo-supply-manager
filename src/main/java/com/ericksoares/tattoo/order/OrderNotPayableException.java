package com.ericksoares.tattoo.order;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

/** Thrown through OrderPayments (payment module) when the order was cancelled. */
public class OrderNotPayableException extends BaseException {

    public OrderNotPayableException(Long orderId, OrderStatus status) {
        super("Order " + orderId + " can't be paid: it is " + status, HttpStatus.CONFLICT);
    }
}
