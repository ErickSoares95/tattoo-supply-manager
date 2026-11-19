package com.ericksoares.tattoo.user;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

class TooManyLoginAttemptsException extends BaseException {

    public TooManyLoginAttemptsException(long minutes) {
        super("Too many failed login attempts. Try again in " + minutes + " minute(s).", HttpStatus.TOO_MANY_REQUESTS);
    }
}
