package com.obraia.backend.ai;

import org.springframework.http.HttpStatus;

import com.obraia.backend.exception.AppException;

/** The AI service failed, did not answer in time, or answered something that cannot be read. */
public class AiServiceException extends AppException {

    public AiServiceException(String message, Throwable cause) {
        this(HttpStatus.BAD_GATEWAY, "AI_SERVICE_FAILED", message, cause);
    }

    /** For more specific failures of the AI service, with their own status and code. */
    protected AiServiceException(HttpStatus status, String code, String message, Throwable cause) {
        super(status, code, message, cause);
    }
}
