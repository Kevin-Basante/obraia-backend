package com.obraia.backend.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Base class for expected application errors. Each one carries the HTTP
 * status and a stable code that the frontend can check. The params hold the
 * values of the message, so the frontend can write it in another language.
 */
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, String> params;

    public AppException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public AppException(HttpStatus status, String code, String message, Throwable cause) {
        this(status, code, message, cause, Map.of());
    }

    public AppException(HttpStatus status, String code, String message, Throwable cause, Map<String, String> params) {
        super(message, cause);
        this.status = status;
        this.code = code;
        this.params = Map.copyOf(params);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Map<String, String> getParams() {
        return params;
    }
}
