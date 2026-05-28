package com.ericksoares.tattoo.user.domain.exception;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BaseException {

    public UserNotFoundException(Long id) {
        super("User not found with id: " + id, HttpStatus.NOT_FOUND);
    }
}