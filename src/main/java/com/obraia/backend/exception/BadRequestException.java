package com.obraia.backend.exception;

import org.springframework.http.HttpStatus;

/** The request has a value the API does not accept, like an unknown filter. */
public class BadRequestException extends AppException {

    public BadRequestException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }
}
