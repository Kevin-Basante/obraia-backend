package com.obraia.backend.dto.budget;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import com.obraia.backend.entity.catalog.ElementType;
import com.obraia.backend.quotation.Quotation;

/**
 * A quotation that is not saved yet.
 *
 * @param workingDaysSource "phases" when the days come from the plan of the project,
 *                          "standard_plan" when they come from the standard phases of its type
 * @param elementsWithoutFormulas measured elements left out because the type has no formulas for them
 */
public record QuotationResponse(BigDecimal projectedAmount, String city, BigDecimal transportPct,
        int workingDays, String workingDaysSource, List<BudgetItemResponse> items,
        List<ProjectMaterialResponse> materials, List<String> elementsWithoutFormulas) {

    public static QuotationResponse from(Quotation quotation) {
        return new QuotationResponse(quotation.total(), quotation.getCity(), quotation.getTransportPct(),
                quotation.getWorkingDays(), quotation.getDaysSource().name().toLowerCase(Locale.ROOT),
                quotation.lines().stream().map(BudgetItemResponse::from).toList(),
                quotation.getMaterials().stream().map(ProjectMaterialResponse::from).toList(),
                quotation.getElementsWithoutFormulas().stream().map(ElementType::getCode).toList());
    }
}
