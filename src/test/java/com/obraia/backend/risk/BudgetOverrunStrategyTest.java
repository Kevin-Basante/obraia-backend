package com.obraia.backend.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BudgetOverrunStrategyTest {

    private final BudgetOverrunStrategy strategy = new BudgetOverrunStrategy();

    private static ProjectSnapshot spent(BigDecimal budget, String spent) {
        return new ProjectSnapshot(LocalDate.of(2026, 11, 20), null, null, BigDecimal.ZERO, List.of(), budget,
                new BigDecimal(spent));
    }

    @ParameterizedTest
    @CsvSource({"500000, 0.00", "900000, 0.00", "950000, 15.00", "1000000, 30.00", "1050000, 62.50",
            "1200000, 100.00", "2000000, 100.00"})
    void scoreGrowsWithTheShareSpent(String spent, String score) {
        RiskFactor factor = strategy.evaluate(spent(new BigDecimal("1000000"), spent));

        assertEquals(new BigDecimal(score), factor.score());
        assertEquals(0, factor.delayDays());
    }

    @Test
    void detailSaysTheShareSpent() {
        assertEquals("105% of the approved budget is spent",
                strategy.evaluate(spent(new BigDecimal("1000000"), "1050000")).detail());
    }

    @Test
    void withoutApprovedBudgetThereIsNothingToCompare() {
        RiskFactor factor = strategy.evaluate(spent(null, "1000000"));

        assertEquals(new BigDecimal("0.00"), factor.score());
        assertEquals("There is no approved budget yet", factor.detail());
    }
}
