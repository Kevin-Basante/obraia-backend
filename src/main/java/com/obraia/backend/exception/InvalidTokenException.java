package com.obraia.backend.exception;

import org.springframework.http.HttpStatus;

/** A refresh token or email link that does not exist, was already used or expired. */
public class InvalidTokenException extends AppException {

    public InvalidTokenException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    public static InvalidTokenException refreshToken() {
        return new InvalidTokenException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN",
                "The session has expired. Sign in again.");
    }

    public static InvalidTokenException verificationLink() {
        return new InvalidTokenException(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_TOKEN",
                "The verification link is invalid or has expired");
    }

    public static InvalidTokenException passwordResetLink() {
        return new InvalidTokenException(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN",
                "The password reset link is invalid or has expired");
    }

    public static InvalidTokenException deletionLink() {
        return new InvalidTokenException(HttpStatus.BAD_REQUEST, "INVALID_DELETION_TOKEN",
                "The deletion link is invalid or has expired");
    }
}
