package com.obraia.backend.dto.overview;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The plan of the work in numbers.
 *
 * @param currentPhase  the first phase that is moving, or the first one still planned; null when there is none
 * @param upcomingTasks the next tasks that are not completed, late ones first
 */
public record WorkOverview(BigDecimal progressPct, LocalDate plannedStartDate, LocalDate plannedEndDate,
        int phaseCount, int completedPhases, int taskCount, int completedTasks, int lateTasks, String currentPhase,
        List<PhaseProgress> phases, List<UpcomingTask> upcomingTasks) {
}
