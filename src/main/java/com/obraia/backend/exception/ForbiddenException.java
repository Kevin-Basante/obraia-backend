package com.obraia.backend.exception;

import org.springframework.http.HttpStatus;

/** The user can see the resource but is not allowed to do this with it. */
public class ForbiddenException extends AppException {

    public ForbiddenException(String code, String message) {
        super(HttpStatus.FORBIDDEN, code, message);
    }
}
