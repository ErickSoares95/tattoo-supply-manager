package com.ericksoares.tattoo.user.domain.exception;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidCurrentPasswordException extends BaseException {

    public InvalidCurrentPasswordException() {
        super("Current password is incorrect", HttpStatus.FORBIDDEN);
    }
}
