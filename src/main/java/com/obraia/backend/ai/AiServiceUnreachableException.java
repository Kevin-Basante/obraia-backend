package com.obraia.backend.ai;

import org.springframework.http.HttpStatus;

/**
 * The call did not reach obraia-ia: the connection failed, or Render
 * answered for it because the service was asleep or still starting. The
 * model was not used, so the call can be tried again once the service
 * answers its health check. The user only has to wait a minute.
 */
public class AiServiceUnreachableException extends AiServiceException {

    public static final String CODE = "AI_SERVICE_WAKING";

    public AiServiceUnreachableException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, CODE, message, cause);
    }
}
