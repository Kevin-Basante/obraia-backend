package com.obraia.backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;

import com.obraia.backend.config.AiProperties;

class AiServiceProxyTest {

    private static final BlueprintAnalysis ANSWER = new BlueprintAnalysis(List.of(), List.of(), 0, "gemini-test",
            false);

    private final UUID projectId = UUID.randomUUID();

    private AiServiceAdapter adapter;
    private ObjectProvider<AiServiceAdapter> provider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        adapter = mock(AiServiceAdapter.class);
        when(adapter.analyze(any())).thenReturn(ANSWER);
        provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(adapter);
    }

    @Test
    void withoutUrlTheAnalysisIsRefusedAndTheAdapterIsNeverCreated() {
        AiServiceProxy proxy = proxy("", 5, 10);

        AiServiceDisabledException ex = assertThrows(AiServiceDisabledException.class,
                () -> proxy.analyze(request("plano", 1)));

        assertFalse(proxy.available());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        verify(provider, never()).getObject();
    }

    @Test
    void theAdapterIsCreatedOnTheFirstCallOnly() {
        AiServiceProxy proxy = proxy("https://obraia-ia.test", 5, 10);
        verify(provider, never()).getObject();

        proxy.analyze(request("plano 1", 1));
        proxy.analyze(request("plano 2", 1));

        assertTrue(proxy.available());
        verify(provider, times(1)).getObject();
        verify(adapter, times(2)).analyze(any());
    }

    private AiServiceProxy proxy(String baseUrl, int maxAnalyses, int cacheSize) {
        AiProperties properties = new AiProperties(baseUrl, "secret", Duration.ofSeconds(1), Duration.ofSeconds(1),
                maxAnalyses, Duration.ofHours(1), cacheSize);
        return new AiServiceProxy(properties, provider);
    }

    private AnalysisRequest request(String content, int floors) {
        return new AnalysisRequest(projectId, "plano.png", bytes(content), floors);
    }

    private static byte[] bytes(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }
}
