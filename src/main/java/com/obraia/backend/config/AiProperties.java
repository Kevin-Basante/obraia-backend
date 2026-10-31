package com.obraia.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the AI service that reads the blueprints (prefix "app.ai").
 *
 * @param baseUrl                URL of obraia-ia; empty turns the analysis off
 * @param serviceToken           shared secret sent in the X-Service-Token header
 * @param connectTimeout         time to open the connection
 * @param readTimeout            time to wait for the answer (the free plan of Render
 *                               sleeps, so the first call can take about a minute)
 * @param maxAnalysesPerProject  calls to the AI a project can make in each window
 * @param limitWindow            length of that window
 * @param cacheSize              analyses kept in memory, so the same file is not read twice
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(String baseUrl, String serviceToken, Duration connectTimeout, Duration readTimeout,
        int maxAnalysesPerProject, Duration limitWindow, int cacheSize) {

    public boolean enabled() {
        return baseUrl != null && !baseUrl.isBlank();
    }
}
