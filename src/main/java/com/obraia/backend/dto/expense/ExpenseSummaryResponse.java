package com.obraia.backend.dto.expense;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.entity.budget.Budget;

/**
 * Spent against planned. Without an approved budget, approvedBudget,
 * remaining and spentPct are null.
 *
 * @param remaining negative when the expenses already passed the budget
 * @param spentPct  share of the approved budget already spent, with 2 decimals
 */
public record ExpenseSummaryResponse(BigDecimal totalSpent, BigDecimal approvedBudget, BigDecimal remaining,
        BigDecimal spentPct, List<CategoryTotal> byCategory) {

    /** Total of one category. */
    public record CategoryTotal(StatusResponse category, BigDecimal amount, long count) {
    }

    public static ExpenseSummaryResponse of(BigDecimal totalSpent, Budget approved, List<CategoryTotal> byCategory) {
        if (approved == null) {
            return new ExpenseSummaryResponse(totalSpent, null, null, null, byCategory);
        }
        BigDecimal planned = approved.getProjectedAmount();
        BigDecimal spentPct = planned.signum() == 0 ? null
                : totalSpent.multiply(BigDecimal.valueOf(100)).divide(planned, 2, RoundingMode.HALF_UP);
        return new ExpenseSummaryResponse(totalSpent, planned, planned.subtract(totalSpent), spentPct, byCategory);
    }
}
