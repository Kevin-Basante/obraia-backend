package com.obraia.backend.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class ScheduleDelayStrategyTest {

    private final ScheduleDelayStrategy strategy = new ScheduleDelayStrategy();

    private static ProjectSnapshot with(List<ProjectSnapshot.LateWork> late) {
        return new ProjectSnapshot(LocalDate.of(2026, 11, 20), null, null, BigDecimal.ZERO, late, null,
                BigDecimal.ZERO);
    }

    @Test
    void nothingLateIsNoRisk() {
        RiskFactor factor = strategy.evaluate(with(List.of()));

        assertEquals("schedule_delay", factor.code());
        assertEquals(new BigDecimal("0.00"), factor.score());
        assertEquals(0, factor.delayDays());
    }

    @Test
    void theWorstTaskGivesTheDelay() {
        RiskFactor factor = strategy.evaluate(with(List.of(new ProjectSnapshot.LateWork("Excavación", 3),
                new ProjectSnapshot.LateWork("Vaciado de zapatas", 6))));

        // 6 days x 4 + 2 tasks x 8
        assertEquals(new BigDecimal("40.00"), factor.score());
        assertEquals(6, factor.delayDays());
        assertEquals("2 tasks are past their planned end; \"Vaciado de zapatas\" is 6 days late", factor.detail());
    }

    @Test
    void scoreStopsAt100() {
        RiskFactor factor = strategy.evaluate(with(List.of(new ProjectSnapshot.LateWork("Muros", 40))));

        assertEquals(new BigDecimal("100.00"), factor.score());
        assertEquals("1 task is past their planned end; \"Muros\" is 40 days late", factor.detail());
    }
}
