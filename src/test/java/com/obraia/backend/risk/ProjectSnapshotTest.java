package com.obraia.backend.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class ProjectSnapshotTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 1);
    private static final LocalDate END = LocalDate.of(2026, 12, 11);

    private static ProjectSnapshot on(LocalDate today, BigDecimal budget, String spent) {
        return new ProjectSnapshot(today, START, END, BigDecimal.ZERO, List.of(), budget, new BigDecimal(spent));
    }

    @Test
    void elapsedTimeGoesFrom0To100() {
        assertEquals(new BigDecimal("0.00"), on(START.minusDays(5), null, "0").elapsedPct());
        assertEquals(new BigDecimal("25.00"), on(START.plusDays(10), null, "0").elapsedPct());
        assertEquals(new BigDecimal("100.00"), on(END.plusDays(30), null, "0").elapsedPct());
        assertEquals(40, on(START, null, "0").plannedDays());
    }

    @Test
    void withoutDatesThereIsNoElapsedTime() {
        ProjectSnapshot snapshot = new ProjectSnapshot(START, null, null, BigDecimal.ZERO, List.of(), null,
                BigDecimal.ZERO);

        assertNull(snapshot.elapsedPct());
        assertEquals(0, snapshot.plannedDays());
    }

    @Test
    void spentShareNeedsAnApprovedBudget() {
        assertNull(on(START, null, "100").spentPct());
        assertEquals(new BigDecimal("12.50"), on(START, new BigDecimal("800000"), "100000").spentPct());
    }
}
