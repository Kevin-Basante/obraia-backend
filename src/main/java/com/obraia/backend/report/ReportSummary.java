package com.obraia.backend.report;

import java.util.List;

/**
 * What the AI wrote about the figures of a report.
 *
 * @param model name of the model that wrote it, shown under the summary
 */
public record ReportSummary(String summary, List<String> recommendations, String model) {

    public ReportSummary {
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }
}
