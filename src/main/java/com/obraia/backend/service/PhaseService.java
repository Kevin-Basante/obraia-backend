package com.obraia.backend.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.phase.PlanPreviewResponse;
import com.obraia.backend.dto.phase.PlannedPhaseResponse;
import com.obraia.backend.dto.phase.WorkPlanResponse;
import com.obraia.backend.entity.phase.Phase;
import com.obraia.backend.entity.phase.Task;
import com.obraia.backend.entity.phase.WorkSchedule;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.planning.PlannedPhase;
import com.obraia.backend.planning.ProjectCalculatorFactories;
import com.obraia.backend.repository.PhaseRepository;
import com.obraia.backend.repository.PhaseTemplateRepository;
import com.obraia.backend.repository.ProgressReportRepository;
import com.obraia.backend.repository.TaskRepository;

/**
 * The phases of a project. The plan is created from the standard phases
 * of the project type, through the factory of that type (Factory Method).
 */
@Service
public class PhaseService {

    private final PhaseRepository phaseRepository;
    private final TaskRepository taskRepository;
    private final ProgressReportRepository reportRepository;
    private final PhaseTemplateRepository templateRepository;
    private final ProjectCalculatorFactories factories;
    private final ProjectAccessService accessService;
    private final WorkPlanViews views;
    private final Clock clock;

    public PhaseService(PhaseRepository phaseRepository, TaskRepository taskRepository,
            ProgressReportRepository reportRepository, PhaseTemplateRepository templateRepository,
            ProjectCalculatorFactories factories, ProjectAccessService accessService, WorkPlanViews views,
            Clock clock) {
        this.phaseRepository = phaseRepository;
        this.taskRepository = taskRepository;
        this.reportRepository = reportRepository;
        this.templateRepository = templateRepository;
        this.factories = factories;
        this.accessService = accessService;
        this.views = views;
        this.clock = clock;
    }

    /** Empty list of phases until the plan is created. */
    @Transactional(readOnly = true)
    public WorkPlanResponse plan(UUID projectId, UUID userId) {
        accessService.requireMembership(projectId, userId);
        return views.plan(projectId);
    }

    /** What {@link #generate} would create, without saving anything. */
    @Transactional(readOnly = true)
    public PlanPreviewResponse preview(UUID projectId, UUID userId) {
        Project project = accessService.requireProject(projectId, userId);
        List<PlannedPhase> phases = plannedPhases(project);
        return new PlanPreviewResponse(project.getProjectType().getName(), project.getSizeCategory().getCode(),
                phases.stream().mapToInt(PlannedPhase::durationDays).sum(),
                phases.stream().map(PlannedPhaseResponse::from).toList());
    }

    @Transactional
    public WorkPlanResponse generate(UUID projectId, UUID userId) {
        Project project = accessService.requireProject(projectId, userId);
        ProjectService.requireEditable(project);
        if (phaseRepository.existsByProjectId(projectId)) {
            throw new ConflictException("PHASES_ALREADY_CREATED",
                    "The project already has phases. Delete the plan first to create it again");
        }
        Instant createdAt = clock.instant();
        int taskNumber = 0;
        for (PlannedPhase planned : plannedPhases(project)) {
            Phase phase = phaseRepository.save(new Phase(project, planned.templateId(), planned.name(),
                    planned.order(), new WorkSchedule(planned.plannedStart(), planned.plannedEnd())));
            for (String name : planned.tasks()) {
                // One microsecond apart, so the tasks keep the order of the plan.
                taskRepository.save(new Task(phase, name,
                        new WorkSchedule(planned.plannedStart(), planned.plannedEnd()),
                        createdAt.plusNanos(1_000L * taskNumber++)));
            }
        }
        taskRepository.flush();
        return views.plan(projectId);
    }

    private List<PlannedPhase> plannedPhases(Project project) {
        LocalDate start = project.getPlannedStartDate();
        return factories.forType(project.getProjectType()).planPhases(
                templateRepository.findByProjectTypeIdAndSizeCategoryOrderBySequenceOrderAsc(
                        project.getProjectType().getId(), project.getSizeCategory()),
                project.getSizeCategory(), start);
    }
}
