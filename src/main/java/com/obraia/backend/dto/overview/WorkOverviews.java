package com.obraia.backend.dto.overview;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.obraia.backend.dto.phase.PhaseResponse;
import com.obraia.backend.dto.phase.TaskResponse;
import com.obraia.backend.dto.phase.WorkPlanResponse;
import com.obraia.backend.entity.phase.WorkStatus;

/** Turns the plan of a project into the numbers of its dashboard. */
public final class WorkOverviews {

    /** How many upcoming tasks the dashboard lists. */
    public static final int UPCOMING_LIMIT = 5;

    private static final String COMPLETED = WorkStatus.COMPLETED.getCode();
    private static final Set<String> MOVING = Set.of(WorkStatus.IN_PROGRESS.getCode(), WorkStatus.DELAYED.getCode());

    private WorkOverviews() {
    }

    public static WorkOverview from(WorkPlanResponse plan) {
        List<PhaseResponse> phases = plan.phases();
        List<TaskResponse> tasks = phases.stream().flatMap(phase -> phase.tasks().stream()).toList();
        return new WorkOverview(plan.progressPct(), plan.plannedStartDate(), plan.plannedEndDate(), phases.size(),
                (int) phases.stream().filter(phase -> isCompleted(phase.status().code())).count(),
                tasks.size(),
                (int) tasks.stream().filter(task -> isCompleted(task.status().code())).count(),
                (int) tasks.stream().filter(TaskResponse::late).count(),
                currentPhase(phases),
                phases.stream().map(PhaseProgress::from).toList(),
                upcoming(phases));
    }

    private static String currentPhase(List<PhaseResponse> phases) {
        return phases.stream()
                .filter(phase -> MOVING.contains(phase.status().code()))
                .findFirst()
                .or(() -> phases.stream().filter(phase -> !isCompleted(phase.status().code())).findFirst())
                .map(PhaseResponse::name)
                .orElse(null);
    }

    /** Late tasks first, then by planned start (tasks without dates last), then in the order of the plan. */
    private static List<UpcomingTask> upcoming(List<PhaseResponse> phases) {
        Comparator<UpcomingTask> order = Comparator.comparing((UpcomingTask task) -> !task.late())
                .thenComparing(UpcomingTask::plannedStartDate, Comparator.nullsLast(Comparator.naturalOrder()));
        return phases.stream()
                .flatMap(phase -> phase.tasks().stream()
                        .filter(task -> !isCompleted(task.status().code()))
                        .map(task -> UpcomingTask.from(task, phase.name())))
                .sorted(order)
                .limit(UPCOMING_LIMIT)
                .toList();
    }

    private static boolean isCompleted(String statusCode) {
        return COMPLETED.equals(statusCode);
    }
}
