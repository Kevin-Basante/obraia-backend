package com.obraia.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for expected application errors. Each one carries the HTTP
 * status and a stable code that the frontend can check.
 */
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public AppException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public AppException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
