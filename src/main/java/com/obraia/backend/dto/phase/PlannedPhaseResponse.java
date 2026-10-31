package com.obraia.backend.dto.phase;

import java.time.LocalDate;
import java.util.List;

import com.obraia.backend.planning.PlannedPhase;

/** A phase the planner proposes, shown before the plan is created. */
public record PlannedPhaseResponse(int order, String name, int durationDays, LocalDate plannedStartDate,
        LocalDate plannedEndDate, List<String> tasks) {

    public static PlannedPhaseResponse from(PlannedPhase phase) {
        return new PlannedPhaseResponse(phase.order(), phase.name(), phase.durationDays(), phase.plannedStart(),
                phase.plannedEnd(), phase.tasks());
    }
}
