package com.ericksoares.tattoo.user;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

/** Correct credentials, but the account is BLOCKED (Spring Security's DisabledException). */
class AccountBlockedException extends BaseException {

    public AccountBlockedException() {
        super("Account is blocked", HttpStatus.FORBIDDEN);
    }
}
