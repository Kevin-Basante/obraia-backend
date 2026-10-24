package com.obraia.backend.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyRegisteredException extends AppException {

    public EmailAlreadyRegisteredException() {
        super(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "An account with this email already exists");
    }
}
