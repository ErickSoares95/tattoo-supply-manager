package com.ericksoares.tattoo.shared.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {

        log.warn("Business error: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                ex.getStatus().value(),
                ex.getMessage()
        );

        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {

        log.error("Unexpected error", ex);

        ErrorResponse error = new ErrorResponse(
                500,
                "Internal server error"
        );

        return ResponseEntity.status(500).body(error);
    }
}