package com.obraia.backend.ai;

import com.obraia.backend.report.ReportSummary;

/**
 * Reads the measurements of a blueprint and writes the summaries of the
 * reports with the AI service. The rest of the backend only knows this
 * interface: {@link AiServiceProxy} is the one injected, and it hands the
 * real calls to {@link AiServiceAdapter}.
 */
public interface AiServiceClient {

    BlueprintAnalysis analyze(AnalysisRequest request);

    /** A short summary with recommendations, written from the figures of a report. */
    ReportSummary summarize(SummaryRequest request);

    /**
     * Asks the AI service if it is up, so the free plan of Render starts it
     * before it is needed. It does not use the model nor the limits.
     */
    void wake();

    /** False when the AI service is not configured, so callers can skip it. */
    default boolean available() {
        return true;
    }
}
