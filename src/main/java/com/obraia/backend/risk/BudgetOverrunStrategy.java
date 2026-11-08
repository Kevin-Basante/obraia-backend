package com.obraia.backend.risk;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Money spent against the approved budget. Up to 90% is normal; from 90%
 * to 100% the score grows slowly, and past 100% it starts at 50.
 */
@Component
@Order(3)
public class BudgetOverrunStrategy implements RiskStrategy {

    static final String CODE = "budget_overrun";
    static final BigDecimal WEIGHT = new BigDecimal("0.25");
    private static final String LABEL = "Spending against budget";
    private static final BigDecimal WARNING_FROM = new BigDecimal("90");
    private static final BigDecimal FULL = new BigDecimal("100");

    @Override
    public RiskFactor evaluate(ProjectSnapshot snapshot) {
        BigDecimal spentPct = snapshot.spentPct();
        if (spentPct == null) {
            return new RiskFactor(CODE, LABEL, RiskScores.ZERO, WEIGHT, 0, "There is no approved budget yet",
                    "no_budget", Map.of());
        }
        String detail = RiskScores.percent(spentPct) + " of the approved budget is spent";
        Map<String, String> params = Map.of("spent", RiskScores.whole(spentPct));
        BigDecimal score;
        if (spentPct.compareTo(WARNING_FROM) <= 0) {
            score = RiskScores.ZERO;
        } else if (spentPct.compareTo(FULL) <= 0) {
            // 3 points for each point past 90%: 30 at 100%.
            score = spentPct.subtract(WARNING_FROM).multiply(BigDecimal.valueOf(3));
        } else {
            // 50 just over the budget, and 100 at 120%.
            score = new BigDecimal("50").add(spentPct.subtract(FULL).multiply(new BigDecimal("2.5")));
        }
        return new RiskFactor(CODE, LABEL, RiskScores.clamp(score), WEIGHT, 0, detail, "budget_spent", params);
    }
}
