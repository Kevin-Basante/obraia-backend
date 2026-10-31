package com.obraia.backend.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class SpendingPaceStrategyTest {

    private final SpendingPaceStrategy strategy = new SpendingPaceStrategy();

    private static ProjectSnapshot with(String progress, BigDecimal budget, String spent) {
        return new ProjectSnapshot(LocalDate.of(2026, 11, 20), null, null, new BigDecimal(progress), List.of(),
                budget, new BigDecimal(spent));
    }

    @Test
    void spendingFasterThanTheWorkGivesScore() {
        RiskFactor factor = strategy.evaluate(with("30", new BigDecimal("1000000"), "600000"));

        // 60% spent - 30% done - 10 of tolerance = 20, twice as score
        assertEquals(new BigDecimal("40.00"), factor.score());
        assertEquals("60% of the budget is spent with 30% of the work done", factor.detail());
    }

    @Test
    void smallGapIsNormal() {
        assertEquals(new BigDecimal("0.00"),
                strategy.evaluate(with("30", new BigDecimal("1000000"), "380000")).score());
    }

    @Test
    void withoutApprovedBudgetThereIsNothingToCompare() {
        assertEquals("There is no approved budget yet", strategy.evaluate(with("30", null, "600000")).detail());
    }
}
