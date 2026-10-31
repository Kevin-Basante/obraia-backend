package com.obraia.backend.service;

import java.time.LocalDate;
import java.util.List;

import com.obraia.backend.entity.phase.Phase;
import com.obraia.backend.entity.phase.Task;
import com.obraia.backend.entity.phase.WorkSchedule;
import com.obraia.backend.entity.phase.WorkStatus;

/**
 * Rules that keep a phase in step with its tasks: it starts when one of
 * its tasks starts and it is completed when all of them are.
 */
final class WorkProgress {

    private WorkProgress() {
    }

    /** A planned task or phase that gets work moves to in progress. */
    static void startIfPlanned(WorkSchedule schedule, LocalDate today) {
        if (schedule.getStatus() == WorkStatus.PLANNED) {
            schedule.changeStatus(WorkStatus.IN_PROGRESS, today);
        }
    }

    /** Updates the phase after one of its tasks changed. */
    static void syncPhase(Phase phase, List<Task> tasks, LocalDate today) {
        WorkSchedule schedule = phase.getSchedule();
        boolean allCompleted = !tasks.isEmpty()
                && tasks.stream().allMatch(task -> task.getSchedule().getStatus() == WorkStatus.COMPLETED);
        boolean anyStarted = tasks.stream().anyMatch(task -> task.getSchedule().getStatus() != WorkStatus.PLANNED);
        if (allCompleted) {
            if (schedule.getStatus() != WorkStatus.COMPLETED) {
                startIfPlanned(schedule, today);
                schedule.changeStatus(WorkStatus.COMPLETED, today);
            }
        } else if (schedule.getStatus() == WorkStatus.COMPLETED) {
            // A task was reopened or added, so the phase is not finished anymore.
            schedule.changeStatus(WorkStatus.IN_PROGRESS, today);
        } else if (anyStarted) {
            startIfPlanned(schedule, today);
        }
    }

    /** Statuses a task or phase can move to, for the error messages. */
    static String allowed(WorkStatus status) {
        List<String> codes = status.nextStatuses().stream().sorted().map(WorkStatus::getCode).toList();
        return codes.isEmpty() ? "none" : String.join(", ", codes);
    }
}
