package com.obraia.backend.ai;

/**
 * Reads the measurements of a blueprint with the AI service. The rest of
 * the backend only knows this interface.
 */
public interface AiServiceClient {

    BlueprintAnalysis analyze(AnalysisRequest request);
}
