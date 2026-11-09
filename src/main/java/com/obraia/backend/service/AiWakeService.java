package com.obraia.backend.service;

import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.obraia.backend.ai.AiServiceClient;
import com.obraia.backend.dto.ai.AiWakeResponse;

/**
 * Wakes the AI service before it is used. The free plan of Render puts it
 * to sleep after a while without calls, and the first call then takes
 * about a minute. The frontend calls this when the user opens a page that
 * may use the AI; the answer comes back at once and the call to obraia-ia
 * goes on in the background.
 */
@Service
public class AiWakeService {

    private static final Logger log = LoggerFactory.getLogger(AiWakeService.class);

    private final AiServiceClient aiClient;
    private final ExecutorService executor;

    public AiWakeService(AiServiceClient aiClient, @Qualifier("aiExecutor") ExecutorService executor) {
        this.aiClient = aiClient;
        this.executor = executor;
    }

    public AiWakeResponse wake() {
        if (!aiClient.available()) {
            return new AiWakeResponse(false);
        }
        executor.execute(() -> {
            try {
                aiClient.wake();
            } catch (RuntimeException ex) {
                // Nothing to do: the next real call will wait for the service as before.
                log.info("The AI service did not answer the wake-up: {}", ex.getMessage());
            }
        });
        return new AiWakeResponse(true);
    }
}
