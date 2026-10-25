package com.obraia.backend.exception;

import org.springframework.http.HttpStatus;

/** An email that the operation depends on could not be sent; the user can try again later. */
public class EmailNotSentException extends AppException {

    public EmailNotSentException(Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_NOT_SENT",
                "We could not send the email. Try again in a few minutes.", cause);
    }
}
