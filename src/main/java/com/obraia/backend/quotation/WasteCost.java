package com.obraia.backend.quotation;

import java.math.BigDecimal;
import java.util.List;

import com.obraia.backend.entity.budget.BudgetItemType;

/** Adds the material bought for breakage and cuts (the waste factor of each formula). */
public class WasteCost extends CostDecorator {

    private final List<MaterialNeed> needs;

    public WasteCost(CostComponent inner, List<MaterialNeed> needs) {
        super(inner);
        this.needs = List.copyOf(needs);
    }

    @Override
    protected CostLine ownLine() {
        BigDecimal amount = needs.stream().map(MaterialNeed::wasteCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CostLine(BudgetItemType.WASTE, "Extra material for breakage and cuts", amount, null);
    }
}
