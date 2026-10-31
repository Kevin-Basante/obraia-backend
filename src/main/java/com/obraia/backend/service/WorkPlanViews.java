package com.obraia.backend.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.obraia.backend.dto.phase.PhaseResponse;
import com.obraia.backend.dto.phase.TaskResponse;
import com.obraia.backend.dto.phase.WorkPlanResponse;
import com.obraia.backend.entity.phase.Phase;
import com.obraia.backend.entity.phase.ProgressReport;
import com.obraia.backend.entity.phase.Task;
import com.obraia.backend.repository.PhaseRepository;
import com.obraia.backend.repository.ProgressReportRepository;
import com.obraia.backend.repository.TaskRepository;

/**
 * Builds the responses of phases and tasks with their progress. The
 * progress of a task is its latest report (by report date, then by the
 * moment it was saved).
 */
@Component
class WorkPlanViews {

    private static final BigDecimal NO_PROGRESS = BigDecimal.ZERO.setScale(2);

    private final PhaseRepository phaseRepository;
    private final TaskRepository taskRepository;
    private final ProgressReportRepository reportRepository;
    private final Clock clock;

    WorkPlanViews(PhaseRepository phaseRepository, TaskRepository taskRepository,
            ProgressReportRepository reportRepository, Clock clock) {
        this.phaseRepository = phaseRepository;
        this.taskRepository = taskRepository;
        this.reportRepository = reportRepository;
        this.clock = clock;
    }

    LocalDate today() {
        return LocalDate.now(clock);
    }

    WorkPlanResponse plan(UUID projectId) {
        Reports reports = reports(projectId);
        Map<UUID, List<Task>> tasksByPhase = taskRepository.findByPhaseProjectIdOrderByCreatedAtAsc(projectId)
                .stream()
                .collect(Collectors.groupingBy(task -> task.getPhase().getId()));
        List<PhaseResponse> phases = phaseRepository.findByProjectIdOrderBySequenceOrderAsc(projectId).stream()
                .map(phase -> phase(phase, tasksByPhase.getOrDefault(phase.getId(), List.of()), reports))
                .toList();
        return WorkPlanResponse.of(phases);
    }

    PhaseResponse phase(Phase phase) {
        UUID projectId = phase.getProject().getId();
        List<Task> tasks = taskRepository.findByPhaseProjectIdOrderByCreatedAtAsc(projectId).stream()
                .filter(task -> task.getPhase().getId().equals(phase.getId()))
                .toList();
        return phase(phase, tasks, reports(projectId));
    }

    TaskResponse task(Task task) {
        return task(task, reports(task.getPhase().getProject().getId()));
    }

    private PhaseResponse phase(Phase phase, List<Task> tasks, Reports reports) {
        return PhaseResponse.from(phase, today(), tasks.stream().map(task -> task(task, reports)).toList());
    }

    private TaskResponse task(Task task, Reports reports) {
        return TaskResponse.from(task, today(), reports.latest().getOrDefault(task.getId(), NO_PROGRESS),
                reports.counts().getOrDefault(task.getId(), 0));
    }

    private Reports reports(UUID projectId) {
        Map<UUID, BigDecimal> latest = new HashMap<>();
        Map<UUID, Integer> counts = new HashMap<>();
        // Newest first, so the first report of each task is its progress.
        for (ProgressReport report : reportRepository.findByTaskPhaseProjectIdOrderByReportDateDescCreatedAtDesc(
                projectId)) {
            UUID taskId = report.getTask().getId();
            latest.putIfAbsent(taskId, report.getCompletionPercentage());
            counts.merge(taskId, 1, Integer::sum);
        }
        return new Reports(latest, counts);
    }

    private record Reports(Map<UUID, BigDecimal> latest, Map<UUID, Integer> counts) {
    }
}
