package com.obraia.backend.ai;

import java.time.Duration;

import org.springframework.http.HttpStatus;

import com.obraia.backend.exception.AppException;

/** The project used all its analyses for now; the free plan of the AI has a quota. */
public class AiLimitReachedException extends AppException {

    public AiLimitReachedException(int maxAnalyses, Duration window) {
        super(HttpStatus.TOO_MANY_REQUESTS, "AI_LIMIT_REACHED", "A project can analyze up to " + maxAnalyses
                + " blueprints every " + window.toMinutes() + " minutes. Try again later");
    }
}
