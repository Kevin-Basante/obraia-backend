package com.obraia.backend.quotation;

import java.math.BigDecimal;
import java.util.List;

import com.obraia.backend.entity.budget.BudgetItemType;

/** Concrete component of the Decorator: the price of the materials, without waste. */
public class MaterialsCost implements CostComponent {

    private final List<MaterialNeed> needs;

    public MaterialsCost(List<MaterialNeed> needs) {
        this.needs = List.copyOf(needs);
    }

    @Override
    public BigDecimal amount() {
        return needs.stream().map(MaterialNeed::baseCost).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public List<CostLine> lines() {
        return List.of(new CostLine(BudgetItemType.MATERIALS,
                "Materials for the measured elements (" + needs.size() + " items)", amount(), null));
    }
}
