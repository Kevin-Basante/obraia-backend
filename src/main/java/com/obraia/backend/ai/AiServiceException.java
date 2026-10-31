package com.obraia.backend.ai;

import org.springframework.http.HttpStatus;

import com.obraia.backend.exception.AppException;

/** The AI service failed, did not answer in time, or answered something that cannot be read. */
public class AiServiceException extends AppException {

    public AiServiceException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, "AI_SERVICE_FAILED", message, cause);
    }
}
