package com.obraia.backend.email;

/** The email provider did not accept the email. */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
