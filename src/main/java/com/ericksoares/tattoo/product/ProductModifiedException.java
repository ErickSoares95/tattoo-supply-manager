package com.ericksoares.tattoo.product;

import com.ericksoares.tattoo.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

/**
 * The product changed (e.g. stock sold by an order) after the client loaded the version it is
 * trying to overwrite.
 */
class ProductModifiedException extends BaseException {

    public ProductModifiedException(Long id) {
        super("Product " + id + " was modified since it was loaded, reload and try again", HttpStatus.CONFLICT);
    }
}
