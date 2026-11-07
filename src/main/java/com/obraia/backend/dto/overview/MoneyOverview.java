package com.obraia.backend.dto.overview;

import java.math.BigDecimal;
import java.util.List;

import com.obraia.backend.dto.expense.ExpenseSummaryResponse;

/**
 * Budget against expenses. Without an approved budget, approvedVersion,
 * approvedBudget, remaining and spentPct are null.
 *
 * @param budgetVersions how many versions were quoted, approved or not
 */
public record MoneyOverview(Integer approvedVersion, int budgetVersions, BigDecimal approvedBudget,
        BigDecimal totalSpent, BigDecimal remaining, BigDecimal spentPct, long expenseCount,
        List<ExpenseSummaryResponse.CategoryTotal> byCategory) {
}
