package com.ericksoares.tattoo.user.domain.exception;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UsernameAlreadyExistsException extends BaseException {

    public UsernameAlreadyExistsException(String username) {
        super("Username already registered: " + username, HttpStatus.CONFLICT);
    }
}