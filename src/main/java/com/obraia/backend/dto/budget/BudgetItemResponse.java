package com.obraia.backend.dto.budget;

import java.math.BigDecimal;
import java.util.Map;

import com.obraia.backend.entity.budget.BudgetItem;
import com.obraia.backend.quotation.CostLine;

/**
 * A row of a budget.
 *
 * @param itemType   code of the layer, like "labor"
 * @param label      text to show for the layer
 * @param params     values of the description, so the frontend can translate it; null when it has none
 * @param percentage rate applied to other rows, or null for a fixed amount
 */
public record BudgetItemResponse(String itemType, String label, String description, Map<String, String> params,
        BigDecimal amount, BigDecimal percentage) {

    public static BudgetItemResponse from(BudgetItem item) {
        return new BudgetItemResponse(item.getItemType().getCode(), item.getItemType().getLabel(),
                item.getDescription(), item.getParams(), item.getAmount(), item.getPercentage());
    }

    public static BudgetItemResponse from(CostLine line) {
        return new BudgetItemResponse(line.type().getCode(), line.type().getLabel(), line.description(),
                line.params().isEmpty() ? null : line.params(), line.amount(), line.percentage());
    }
}
