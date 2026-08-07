package com.ericksoares.tattoo.payment.domain.exception;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class OrderAlreadyPaidException extends BaseException {

    public OrderAlreadyPaidException(Long orderId) {
        super("Order already has an approved payment: " + orderId, HttpStatus.CONFLICT);
    }
}
