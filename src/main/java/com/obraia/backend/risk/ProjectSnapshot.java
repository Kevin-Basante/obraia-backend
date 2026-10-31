package com.obraia.backend.risk;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Everything the risk strategies need to know about a project, read once.
 *
 * @param plannedStart   start of the plan; null when the project has no phases with dates
 * @param plannedEnd     end of the plan; null when the project has no phases with dates
 * @param progressPct    average progress of the tasks, 0 to 100
 * @param lateWork       tasks not completed whose planned end already passed
 * @param approvedBudget projected amount of the approved budget; null without one
 * @param spent          total of the expenses
 */
public record ProjectSnapshot(LocalDate today, LocalDate plannedStart, LocalDate plannedEnd, BigDecimal progressPct,
        List<LateWork> lateWork, BigDecimal approvedBudget, BigDecimal spent) {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** A task that should be finished by now. */
    public record LateWork(String name, int daysLate) {
    }

    public boolean hasDates() {
        return plannedStart != null && plannedEnd != null && plannedEnd.isAfter(plannedStart);
    }

    /** Days between the planned start and end; 0 without dates. */
    public long plannedDays() {
        return hasDates() ? ChronoUnit.DAYS.between(plannedStart, plannedEnd) : 0;
    }

    /** Share of the planned time already passed, 0 to 100; null without dates. */
    public BigDecimal elapsedPct() {
        if (!hasDates()) {
            return null;
        }
        long passed = Math.max(0, Math.min(ChronoUnit.DAYS.between(plannedStart, today), plannedDays()));
        return BigDecimal.valueOf(passed).multiply(HUNDRED).divide(BigDecimal.valueOf(plannedDays()), 2,
                RoundingMode.HALF_UP);
    }

    /** Share of the approved budget already spent; null without an approved budget. */
    public BigDecimal spentPct() {
        if (approvedBudget == null || approvedBudget.signum() == 0) {
            return null;
        }
        return spent.multiply(HUNDRED).divide(approvedBudget, 2, RoundingMode.HALF_UP);
    }
}
