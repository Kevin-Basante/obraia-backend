package com.obraia.backend.report;

import java.time.Instant;

import com.obraia.backend.dto.overview.ActivityCounts;
import com.obraia.backend.dto.overview.MoneyOverview;
import com.obraia.backend.dto.overview.WorkOverview;
import com.obraia.backend.dto.project.ProjectSummaryResponse;
import com.obraia.backend.dto.risk.RiskAssessmentResponse;

/**
 * Everything one report shows. The public report leaves money, risk and
 * counts null, so they cannot end up in its PDF by mistake.
 *
 * @param risk    null in the public report, or before the first assessment
 * @param summary null when the AI is off or did not answer
 */
public record ReportData(ReportType type, ProjectSummaryResponse project, int floors, WorkOverview work,
        MoneyOverview money, RiskAssessmentResponse risk, ActivityCounts counts, Instant generatedAt,
        ReportSummary summary) {

    /** The same data with the summary of the AI. */
    public ReportData withSummary(ReportSummary newSummary) {
        return new ReportData(type, project, floors, work, money, risk, counts, generatedAt, newSummary);
    }
}
