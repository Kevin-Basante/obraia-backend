package com.obraia.backend.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class ProgressPaceStrategyTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 1);

    private final ProgressPaceStrategy strategy = new ProgressPaceStrategy();

    private static ProjectSnapshot on(int day, String progress) {
        return new ProjectSnapshot(START.plusDays(day), START, START.plusDays(100), new BigDecimal(progress),
                List.of(), null, BigDecimal.ZERO);
    }

    @Test
    void behindTheTimeGivesScoreAndDelay() {
        RiskFactor factor = strategy.evaluate(on(50, "30"));

        // 20 points behind, twice as score; 20% of 100 planned days
        assertEquals(new BigDecimal("40.00"), factor.score());
        assertEquals(20, factor.delayDays());
        assertEquals("50% of the planned time passed with 30% of the work done", factor.detail());
    }

    @Test
    void aheadOfTimeIsNoRisk() {
        RiskFactor factor = strategy.evaluate(on(20, "35"));

        assertEquals(new BigDecimal("0.00"), factor.score());
        assertEquals(0, factor.delayDays());
    }

    @Test
    void withoutDatesThereIsNothingToCompare() {
        ProjectSnapshot snapshot = new ProjectSnapshot(START, null, null, BigDecimal.ZERO, List.of(), null,
                BigDecimal.ZERO);

        assertEquals("The plan has no dates yet", strategy.evaluate(snapshot).detail());
    }
}
