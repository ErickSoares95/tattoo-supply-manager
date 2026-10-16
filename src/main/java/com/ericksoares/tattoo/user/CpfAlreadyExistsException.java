package com.ericksoares.tattoo.user;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

class CpfAlreadyExistsException extends BaseException {

    public CpfAlreadyExistsException(String cpf) {
        super("CPF already registered: " + cpf, HttpStatus.CONFLICT);
    }
}
