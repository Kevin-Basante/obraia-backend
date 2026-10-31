package com.obraia.backend.ai;

/**
 * Reads the measurements of a blueprint with the AI service. The rest of
 * the backend only knows this interface: {@link AiServiceProxy} is the one
 * injected, and it hands the real calls to {@link AiServiceAdapter}.
 */
public interface AiServiceClient {

    BlueprintAnalysis analyze(AnalysisRequest request);

    /** False when the AI service is not configured, so callers can skip it. */
    default boolean available() {
        return true;
    }
}
