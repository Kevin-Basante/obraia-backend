package com.obraia.backend.ai;

import java.util.Map;
import java.util.UUID;

/**
 * What the AI needs to write the summary of a report.
 *
 * @param projectId  only used for the limit of calls per project; it is not sent
 * @param language   "en" or "es": the language of the answer
 * @param reportType "public" or "full"
 * @param figures    the numbers of the report, with the snake_case names obraia-ia expects
 */
public record SummaryRequest(UUID projectId, String language, String reportType, Map<String, Object> figures) {
}
