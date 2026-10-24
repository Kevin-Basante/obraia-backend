package com.obraia.backend.exception;

import org.springframework.http.HttpStatus;

/** Same message for unknown email and wrong password, so nobody can find out which emails exist. */
public class InvalidCredentialsException extends AppException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "The email or password is incorrect");
    }
}
