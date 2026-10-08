package com.obraia.backend.exception;

import org.springframework.http.HttpStatus;

/** The database did not answer (stopped, unreachable or wrong credentials). */
public class DatabaseUnavailableException extends AppException {

    public DatabaseUnavailableException(Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE,
                "DATABASE_UNAVAILABLE",
                "The database is not available. Try again in a few seconds.",
                cause);
    }
}
