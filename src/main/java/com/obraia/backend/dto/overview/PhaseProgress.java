package com.obraia.backend.dto.overview;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.obraia.backend.dto.phase.PhaseResponse;
import com.obraia.backend.dto.project.StatusResponse;

/** One phase on the dashboard: how far it went, without its tasks. */
public record PhaseProgress(UUID id, String name, int order, StatusResponse status, BigDecimal progressPct,
        boolean late, LocalDate plannedStartDate, LocalDate plannedEndDate, int taskCount) {

    public static PhaseProgress from(PhaseResponse phase) {
        return new PhaseProgress(phase.id(), phase.name(), phase.order(), phase.status(), phase.progressPct(),
                phase.late(), phase.plannedStartDate(), phase.plannedEndDate(), phase.tasks().size());
    }
}
