package com.obraia.backend.dto.phase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.entity.phase.Task;
import com.obraia.backend.entity.phase.WorkSchedule;

/**
 * A task with its progress: the percentage of its latest report, or 0
 * when nobody reported it yet.
 */
public record TaskResponse(UUID id, UUID phaseId, String name, StatusResponse status,
        List<StatusResponse> nextStatuses, LocalDate plannedStartDate, LocalDate plannedEndDate,
        LocalDate actualStartDate, LocalDate actualEndDate, boolean late, BigDecimal progressPct, int reportCount) {

    public static TaskResponse from(Task task, LocalDate today, BigDecimal progressPct, int reportCount) {
        WorkSchedule schedule = task.getSchedule();
        return new TaskResponse(task.getId(), task.getPhase().getId(), task.getName(),
                WorkStatuses.status(schedule.getStatus()), WorkStatuses.next(schedule.getStatus()),
                schedule.getPlannedStartDate(), schedule.getPlannedEndDate(), schedule.getActualStartDate(),
                schedule.getActualEndDate(), schedule.isLate(today), progressPct, reportCount);
    }
}
