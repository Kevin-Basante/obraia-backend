package com.obraia.backend.dto.phase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.entity.phase.Phase;
import com.obraia.backend.entity.phase.WorkSchedule;

/** A phase with its tasks. Its progress is the average of the progress of its tasks. */
public record PhaseResponse(UUID id, String name, int order, StatusResponse status,
        List<StatusResponse> nextStatuses, LocalDate plannedStartDate, LocalDate plannedEndDate,
        LocalDate actualStartDate, LocalDate actualEndDate, boolean late, BigDecimal progressPct,
        List<TaskResponse> tasks) {

    public static PhaseResponse from(Phase phase, LocalDate today, List<TaskResponse> tasks) {
        WorkSchedule schedule = phase.getSchedule();
        return new PhaseResponse(phase.getId(), phase.getName(), phase.getSequenceOrder(),
                WorkStatuses.status(schedule.getStatus()), WorkStatuses.next(schedule.getStatus()),
                schedule.getPlannedStartDate(), schedule.getPlannedEndDate(), schedule.getActualStartDate(),
                schedule.getActualEndDate(), schedule.isLate(today),
                Progress.average(tasks.stream().map(TaskResponse::progressPct).toList()), tasks);
    }
}
