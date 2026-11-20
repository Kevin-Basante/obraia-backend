package com.obraia.backend.service;

import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.obraia.backend.ai.AiServiceClient;
import com.obraia.backend.config.AiProperties;
import com.obraia.backend.dto.ai.AiWakeResponse;

/**
 * Wakes the AI service before it is used. The free plan of Render puts it
 * to sleep after a while without calls, and the first call then takes
 * about a minute. The frontend calls this while a signed-in user uses the
 * site; the answer comes back at once and the call to obraia-ia goes on in
 * the background. Render does not wake the service for calls from this
 * backend, so the answer also gives the browser the URL to visit.
 */
@Service
public class AiWakeService {

    private static final Logger log = LoggerFactory.getLogger(AiWakeService.class);

    static final String HEALTH_PATH = "/health";

    private final AiServiceClient aiClient;
    private final AiProperties properties;
    private final ExecutorService executor;

    public AiWakeService(AiServiceClient aiClient, AiProperties properties,
            @Qualifier("aiExecutor") ExecutorService executor) {
        this.aiClient = aiClient;
        this.properties = properties;
        this.executor = executor;
    }

    public AiWakeResponse wake() {
        if (!aiClient.available()) {
            return new AiWakeResponse(false, null);
        }
        executor.execute(() -> {
            try {
                aiClient.wake();
            } catch (RuntimeException ex) {
                // Nothing to do: the next real call will wait for the service as before.
                log.info("The AI service did not answer the wake-up: {}", ex.getMessage());
            }
        });
        return new AiWakeResponse(true, properties.baseUrl().strip().replaceAll("/+$", "") + HEALTH_PATH);
    }
}
