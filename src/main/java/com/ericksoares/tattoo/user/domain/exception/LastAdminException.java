package com.ericksoares.tattoo.user.domain.exception;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class LastAdminException extends BaseException {

    public LastAdminException() {
        super("Cannot remove or demote the last active admin", HttpStatus.CONFLICT);
    }
}
