package com.ericksoares.tattoo.user;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

class InvalidCurrentPasswordException extends BaseException {

    public InvalidCurrentPasswordException() {
        super("Current password is incorrect", HttpStatus.FORBIDDEN);
    }
}
