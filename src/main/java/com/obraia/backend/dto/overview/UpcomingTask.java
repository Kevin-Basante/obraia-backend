package com.obraia.backend.dto.overview;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.obraia.backend.dto.phase.TaskResponse;
import com.obraia.backend.dto.project.StatusResponse;

/** A task that is not completed yet, with the name of its phase. */
public record UpcomingTask(UUID id, String name, String phaseName, StatusResponse status,
        LocalDate plannedStartDate, LocalDate plannedEndDate, BigDecimal progressPct, boolean late) {

    public static UpcomingTask from(TaskResponse task, String phaseName) {
        return new UpcomingTask(task.id(), task.name(), phaseName, task.status(), task.plannedStartDate(),
                task.plannedEndDate(), task.progressPct(), task.late());
    }
}
