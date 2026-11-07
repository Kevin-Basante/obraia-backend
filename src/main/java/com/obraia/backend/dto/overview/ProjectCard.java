package com.obraia.backend.dto.overview;

import java.math.BigDecimal;

import com.obraia.backend.dto.project.ProjectSummaryResponse;

/**
 * One project on the main dashboard.
 *
 * @param approvedBudget null without an approved budget
 * @param spentPct       null without an approved budget
 * @param risk           null before the first risk assessment
 */
public record ProjectCard(ProjectSummaryResponse project, BigDecimal progressPct, BigDecimal approvedBudget,
        BigDecimal totalSpent, BigDecimal spentPct, RiskBadge risk) {
}
