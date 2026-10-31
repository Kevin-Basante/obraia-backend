package com.obraia.backend.dto.budget;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.entity.budget.Budget;

/** A budget version without its rows, for the list of versions. */
public record BudgetSummaryResponse(UUID id, int version, StatusResponse status, BigDecimal projectedAmount,
        BigDecimal actualAmount, Instant createdAt, Instant approvedAt) {

    public static BudgetSummaryResponse from(Budget budget) {
        return new BudgetSummaryResponse(budget.getId(), budget.getVersion(),
                new StatusResponse(budget.getStatus().getCode(), budget.getStatus().getLabel()),
                budget.getProjectedAmount(), budget.getActualAmount(), budget.getCreatedAt(),
                budget.getApprovedAt());
    }
}
