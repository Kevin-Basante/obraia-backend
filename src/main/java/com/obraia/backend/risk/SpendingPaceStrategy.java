package com.obraia.backend.risk;

import java.math.BigDecimal;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Money against progress: spending 60% of the budget with 30% of the work
 * done means the rest will probably cost more than planned. A gap of up to
 * 10 points is normal (materials are bought before they are used).
 */
@Component
@Order(4)
public class SpendingPaceStrategy implements RiskStrategy {

    static final String CODE = "spending_pace";
    static final BigDecimal WEIGHT = new BigDecimal("0.15");
    private static final String LABEL = "Spending against progress";
    private static final BigDecimal TOLERANCE = BigDecimal.TEN;

    @Override
    public RiskFactor evaluate(ProjectSnapshot snapshot) {
        BigDecimal spentPct = snapshot.spentPct();
        if (spentPct == null) {
            return new RiskFactor(CODE, LABEL, RiskScores.ZERO, WEIGHT, 0, "There is no approved budget yet");
        }
        String detail = RiskScores.percent(spentPct) + " of the budget is spent with "
                + RiskScores.percent(snapshot.progressPct()) + " of the work done";
        BigDecimal gap = spentPct.subtract(snapshot.progressPct()).subtract(TOLERANCE);
        return new RiskFactor(CODE, LABEL, RiskScores.clamp(gap.multiply(BigDecimal.valueOf(2))), WEIGHT, 0, detail);
    }
}
