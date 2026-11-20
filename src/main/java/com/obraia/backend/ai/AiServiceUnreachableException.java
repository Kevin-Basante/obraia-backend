package com.obraia.backend.ai;

import org.springframework.http.HttpStatus;

/**
 * The call did not reach obraia-ia: the connection failed, or Render
 * answered with its own error page because the service was asleep or still
 * starting. The model was not used, so the call can be tried again once the
 * service answers its health check.
 */
public class AiServiceUnreachableException extends AiServiceException {

    public AiServiceUnreachableException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, "AI_SERVICE_FAILED", message, cause);
    }
}
