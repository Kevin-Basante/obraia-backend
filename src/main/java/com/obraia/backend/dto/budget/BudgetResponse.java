package com.obraia.backend.dto.budget;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.entity.budget.Budget;
import com.obraia.backend.entity.budget.BudgetItem;

/** A budget version with its rows, in the order the layers were added. */
public record BudgetResponse(UUID id, int version, StatusResponse status, BigDecimal projectedAmount,
        BigDecimal actualAmount, Instant createdAt, Instant approvedAt, List<BudgetItemResponse> items) {

    public static BudgetResponse from(Budget budget, List<BudgetItem> items) {
        BudgetSummaryResponse summary = BudgetSummaryResponse.from(budget);
        return new BudgetResponse(summary.id(), summary.version(), summary.status(), summary.projectedAmount(),
                summary.actualAmount(), summary.createdAt(), summary.approvedAt(),
                items.stream().map(BudgetItemResponse::from).toList());
    }
}
