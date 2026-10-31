package com.obraia.backend.ai;

import org.springframework.http.HttpStatus;

import com.obraia.backend.exception.AppException;

/** AI_SERVICE_URL is not set, so the blueprints cannot be read. */
public class AiServiceDisabledException extends AppException {

    public AiServiceDisabledException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_DISABLED", "The AI service is not configured");
    }
}
