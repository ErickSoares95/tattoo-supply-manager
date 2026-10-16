package com.ericksoares.tattoo.product;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

/**
 * The stock kept changing under concurrent orders and the optimistic-lock retries ran out.
 */
public class StockConflictException extends BaseException {

    public StockConflictException() {
        super("Stock was modified by another order, please try again", HttpStatus.CONFLICT);
    }
}
