package com.obraia.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.obraia.backend.ai.AiServiceAdapter;

/**
 * The adapter of the AI service is lazy: Spring creates it the first time
 * the proxy asks for it, never at startup.
 */
@Configuration
public class AiConfig {

    @Bean
    @Lazy
    public AiServiceAdapter aiServiceAdapter(AiProperties properties) {
        return AiServiceAdapter.create(properties);
    }
}
