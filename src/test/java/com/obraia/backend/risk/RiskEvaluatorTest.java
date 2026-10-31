package com.obraia.backend.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.obraia.backend.entity.risk.RiskLevel;

class RiskEvaluatorTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 1);

    private final RiskEvaluator evaluator = new RiskEvaluator(List.of(new ScheduleDelayStrategy(),
            new ProgressPaceStrategy(), new BudgetOverrunStrategy(), new SpendingPaceStrategy()));

    @Test
    void healthyProjectIsLow() {
        ProjectSnapshot snapshot = new ProjectSnapshot(START.plusDays(20), START, START.plusDays(100),
                new BigDecimal("25"), List.of(), new BigDecimal("1000000"), new BigDecimal("250000"));

        RiskResult result = evaluator.evaluate(snapshot);

        assertEquals(new BigDecimal("0.00"), result.score());
        assertEquals(RiskLevel.LOW, result.level());
        assertEquals(0, result.predictedDelayDays());
        assertEquals(List.of("schedule_delay", "progress_pace", "budget_overrun", "spending_pace"),
                result.factors().stream().map(RiskFactor::code).toList());
    }

    @Test
    void oneSeriousProblemIsEnoughToRaiseTheLevel() {
        // Only the budget is wrong: 120% spent gives 100 in that factor.
        ProjectSnapshot snapshot = new ProjectSnapshot(START.plusDays(20), START, START.plusDays(100),
                new BigDecimal("25"), List.of(), new BigDecimal("1000000"), new BigDecimal("1200000"));

        RiskResult result = evaluator.evaluate(snapshot);

        // budget 100 x 0.25 + spending pace 100 x 0.15 = 40 of average; half of 40 plus half of 100
        assertEquals(new BigDecimal("70.00"), result.score());
        assertEquals(RiskLevel.HIGH, result.level());
    }

    @Test
    void lateProjectPredictsTheLongestDelay() {
        ProjectSnapshot snapshot = new ProjectSnapshot(START.plusDays(60), START, START.plusDays(100),
                new BigDecimal("20"), List.of(new ProjectSnapshot.LateWork("Muros", 12)), null, BigDecimal.ZERO);

        RiskResult result = evaluator.evaluate(snapshot);

        // progress pace: 40 points behind = 40 days, more than the 12 of the late task
        assertEquals(40, result.predictedDelayDays());
        // late tasks 56 x 0.35 + progress 80 x 0.25 = 39.6 of average; half of it plus half of 80
        assertEquals(new BigDecimal("59.80"), result.score());
        assertEquals(RiskLevel.HIGH, result.level());
    }

    @Test
    void withoutStrategiesTheRiskIsZero() {
        RiskResult result = new RiskEvaluator(List.of()).evaluate(new ProjectSnapshot(START, null, null,
                BigDecimal.ZERO, List.of(), null, BigDecimal.ZERO));

        assertEquals(new BigDecimal("0.00"), result.score());
        assertEquals(RiskLevel.LOW, result.level());
    }
}
