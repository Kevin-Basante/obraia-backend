package com.obraia.backend.ai;

import java.util.UUID;

/**
 * A blueprint to read.
 *
 * @param projectId project it belongs to; the proxy counts the analyses of each project
 * @param floors    floors of the project, so the AI does not invent a second floor
 */
public record AnalysisRequest(UUID projectId, String filename, byte[] content, int floors) {
}
