package com.obraia.backend.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

/** The request is valid but the current state does not allow it, like editing a finished project. */
public class ConflictException extends AppException {

    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }

    public ConflictException(String code, String message, Map<String, String> params) {
        super(HttpStatus.CONFLICT, code, message, null, params);
    }
}
