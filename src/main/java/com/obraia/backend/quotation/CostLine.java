package com.obraia.backend.quotation;

import java.math.BigDecimal;

import com.obraia.backend.entity.budget.BudgetItemType;

/**
 * One layer of the cost, saved as a row of the budget.
 *
 * @param percentage rate applied to other rows, or null for a fixed amount
 */
public record CostLine(BudgetItemType type, String description, BigDecimal amount, BigDecimal percentage) {
}
