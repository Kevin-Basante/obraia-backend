package com.obraia.backend.quotation;

import java.math.BigDecimal;
import java.util.Map;

import com.obraia.backend.entity.budget.BudgetItemType;

/**
 * One layer of the cost, saved as a row of the budget.
 *
 * @param percentage rate applied to other rows, or null for a fixed amount
 * @param params     values of the description, so the frontend can translate it; empty when it has none
 */
public record CostLine(BudgetItemType type, String description, BigDecimal amount, BigDecimal percentage,
        Map<String, String> params) {

    /** A line whose description has no values. */
    public CostLine(BudgetItemType type, String description, BigDecimal amount, BigDecimal percentage) {
        this(type, description, amount, percentage, Map.of());
    }
}
