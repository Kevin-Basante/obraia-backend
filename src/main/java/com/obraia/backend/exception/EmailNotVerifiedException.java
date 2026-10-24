package com.obraia.backend.exception;

import org.springframework.http.HttpStatus;

public class EmailNotVerifiedException extends AppException {

    public EmailNotVerifiedException() {
        super(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED",
                "Verify your email before signing in. Check your inbox or request a new link.");
    }
}
