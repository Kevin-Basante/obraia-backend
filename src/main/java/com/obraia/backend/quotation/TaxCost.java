package com.obraia.backend.quotation;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

import com.obraia.backend.entity.budget.BudgetItemType;

/**
 * Adds the VAT of the services. Material prices already include it, so
 * it is only charged on labor and transport.
 */
public class TaxCost extends CostDecorator {

    private static final Set<BudgetItemType> BASE = EnumSet.of(BudgetItemType.LABOR, BudgetItemType.TRANSPORT);

    private final BigDecimal vatPct;

    public TaxCost(CostComponent inner, BigDecimal vatPct) {
        super(inner);
        this.vatPct = vatPct;
    }

    @Override
    protected CostLine ownLine() {
        return new CostLine(BudgetItemType.TAX, "VAT on labor and transport (material prices include it)",
                Money.percentOf(innerAmountOf(BASE), vatPct), vatPct);
    }
}
