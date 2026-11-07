package com.obraia.backend.dto.overview;

import java.util.UUID;

/**
 * Everything the dashboard of a project shows, in one answer.
 *
 * @param risk null before the first risk assessment
 */
public record ProjectOverviewResponse(UUID projectId, WorkOverview work, MoneyOverview money, RiskBadge risk,
        ActivityCounts counts) {
}
