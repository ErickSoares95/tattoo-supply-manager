package com.ericksoares.tattoo.user.domain.exception;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends BaseException {

    public InvalidCredentialsException() {
        super("Invalid login or password", HttpStatus.UNAUTHORIZED);
    }
}
