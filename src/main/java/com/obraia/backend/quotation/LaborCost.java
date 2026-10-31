package com.obraia.backend.quotation;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import com.obraia.backend.entity.budget.BudgetItemType;

/** Adds the crew: working days of the plan times the daily rate. */
public class LaborCost extends CostDecorator {

    private final int workingDays;
    private final BigDecimal dailyRate;

    public LaborCost(CostComponent inner, int workingDays, BigDecimal dailyRate) {
        super(inner);
        this.workingDays = workingDays;
        this.dailyRate = dailyRate;
    }

    @Override
    protected CostLine ownLine() {
        String rate = NumberFormat.getIntegerInstance(Locale.US).format(dailyRate);
        return new CostLine(BudgetItemType.LABOR,
                "Crew for " + workingDays + " working days at COP " + rate + " a day",
                Money.round(dailyRate.multiply(BigDecimal.valueOf(workingDays))), null);
    }
}
