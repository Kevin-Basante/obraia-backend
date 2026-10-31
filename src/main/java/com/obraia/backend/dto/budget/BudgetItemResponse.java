package com.obraia.backend.dto.budget;

import java.math.BigDecimal;

import com.obraia.backend.entity.budget.BudgetItem;
import com.obraia.backend.quotation.CostLine;

/**
 * A row of a budget.
 *
 * @param itemType   code of the layer, like "labor"
 * @param label      text to show for the layer
 * @param percentage rate applied to other rows, or null for a fixed amount
 */
public record BudgetItemResponse(String itemType, String label, String description, BigDecimal amount,
        BigDecimal percentage) {

    public static BudgetItemResponse from(BudgetItem item) {
        return new BudgetItemResponse(item.getItemType().getCode(), item.getItemType().getLabel(),
                item.getDescription(), item.getAmount(), item.getPercentage());
    }

    public static BudgetItemResponse from(CostLine line) {
        return new BudgetItemResponse(line.type().getCode(), line.type().getLabel(), line.description(),
                line.amount(), line.percentage());
    }
}
