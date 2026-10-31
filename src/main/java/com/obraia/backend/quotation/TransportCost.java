package com.obraia.backend.quotation;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

import com.obraia.backend.entity.budget.BudgetItemType;

/** Adds the cost of bringing the materials to the site: a percentage that depends on the city. */
public class TransportCost extends CostDecorator {

    private static final Set<BudgetItemType> BASE = EnumSet.of(BudgetItemType.MATERIALS, BudgetItemType.WASTE);

    private final BigDecimal transportPct;
    private final String city;

    public TransportCost(CostComponent inner, BigDecimal transportPct, String city) {
        super(inner);
        this.transportPct = transportPct;
        this.city = city;
    }

    @Override
    protected CostLine ownLine() {
        return new CostLine(BudgetItemType.TRANSPORT, "Transport of the materials to the site in " + city,
                Money.percentOf(innerAmountOf(BASE), transportPct), transportPct);
    }
}
