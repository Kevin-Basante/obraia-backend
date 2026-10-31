package com.obraia.backend.ai;

import org.springframework.http.HttpStatus;

/**
 * The AI service is up, but the model is overloaded or out of quota for
 * now. It is still a failure of the analysis (the blueprint is marked as
 * failed), but the user only has to try again a bit later.
 */
public class AiServiceBusyException extends AiServiceException {

    public static final String MESSAGE = "The AI is busy right now. Try again in a few minutes";

    public AiServiceBusyException(Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_BUSY", MESSAGE, cause);
    }
}
