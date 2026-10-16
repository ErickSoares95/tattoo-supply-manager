package com.ericksoares.tattoo.user;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

class InvalidCredentialsException extends BaseException {

    public InvalidCredentialsException() {
        super("Invalid login or password", HttpStatus.UNAUTHORIZED);
    }
}
