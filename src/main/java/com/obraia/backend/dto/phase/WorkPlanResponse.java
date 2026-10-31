package com.obraia.backend.dto.phase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The phases of a project. progressPct is the average of every task, so
 * a phase with more tasks weighs more. The dates are the ends of the plan.
 */
public record WorkPlanResponse(BigDecimal progressPct, LocalDate plannedStartDate, LocalDate plannedEndDate,
        List<PhaseResponse> phases) {

    public static WorkPlanResponse of(List<PhaseResponse> phases) {
        List<BigDecimal> taskProgress = phases.stream()
                .flatMap(phase -> phase.tasks().stream())
                .map(TaskResponse::progressPct)
                .toList();
        LocalDate start = phases.isEmpty() ? null : phases.get(0).plannedStartDate();
        LocalDate end = phases.isEmpty() ? null : phases.get(phases.size() - 1).plannedEndDate();
        return new WorkPlanResponse(Progress.average(taskProgress), start, end, phases);
    }
}
