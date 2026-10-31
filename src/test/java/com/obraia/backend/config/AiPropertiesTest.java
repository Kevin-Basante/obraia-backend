package com.obraia.backend.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class AiPropertiesTest {

    @Test
    void theAnalysisIsOnOnlyWithAUrl() {
        assertTrue(properties("https://obraia-ia.onrender.com").enabled());
        assertFalse(properties("").enabled());
        assertFalse(properties("   ").enabled());
        assertFalse(properties(null).enabled());
    }

    private static AiProperties properties(String baseUrl) {
        return new AiProperties(baseUrl, "token", Duration.ofSeconds(1), Duration.ofSeconds(1), 5,
                Duration.ofHours(1), 10);
    }
}
