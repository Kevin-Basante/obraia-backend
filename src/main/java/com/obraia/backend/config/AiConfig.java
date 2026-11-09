package com.obraia.backend.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.obraia.backend.ai.AiServiceAdapter;

/**
 * The adapter of the AI service is lazy: Spring creates it the first time
 * the proxy asks for it, never at startup. The calls that must not hold the
 * request (wake-ups, summaries with a time limit) run on their own threads.
 */
@Configuration
public class AiConfig {

    @Bean
    @Lazy
    public AiServiceAdapter aiServiceAdapter(AiProperties properties) {
        return AiServiceAdapter.create(properties);
    }

    /** Threads for the calls to the AI that run in the background; they do not keep the app alive. */
    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService aiExecutor() {
        return Executors.newCachedThreadPool(task -> {
            Thread thread = new Thread(task, "ai-call");
            thread.setDaemon(true);
            return thread;
        });
    }
}
