package com.obraia.backend.dto.overview;

import java.math.BigDecimal;
import java.util.List;

/**
 * All the projects of the user, with the totals on top.
 *
 * @param active         projects in progress
 * @param atRisk         projects whose latest risk is high or critical
 * @param approvedBudget sum of the approved budgets
 * @param totalSpent     sum of the expenses of every project
 */
public record PortfolioResponse(int projectCount, int active, int atRisk, BigDecimal approvedBudget,
        BigDecimal totalSpent, List<ProjectCard> projects) {
}
