package com.ericksoares.tattoo.user;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

class UserHasOrdersException extends BaseException {

    public UserHasOrdersException(Long id) {
        super("User " + id + " has orders and can't be deleted; block the account instead", HttpStatus.CONFLICT);
    }
}
