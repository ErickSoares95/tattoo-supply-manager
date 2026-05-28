package com.ericksoares.tattoo.user.domain.exception;

public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String username) {
        super("Username already registered: " + username);
    }
}