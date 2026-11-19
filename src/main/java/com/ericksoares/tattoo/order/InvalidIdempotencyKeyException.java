package com.ericksoares.tattoo.order;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

class InvalidIdempotencyKeyException extends BaseException {

    public InvalidIdempotencyKeyException() {
        super("Idempotency-Key must be at most 100 characters", HttpStatus.BAD_REQUEST);
    }
}
