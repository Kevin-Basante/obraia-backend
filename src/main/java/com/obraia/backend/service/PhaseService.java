package com.obraia.backend.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.phase.PhaseRequest;
import com.obraia.backend.dto.phase.PhaseResponse;
import com.obraia.backend.dto.phase.PlanPreviewResponse;
import com.obraia.backend.dto.phase.PlannedPhaseResponse;
import com.obraia.backend.dto.phase.WorkPlanResponse;
import com.obraia.backend.entity.phase.Phase;
import com.obraia.backend.entity.phase.Task;
import com.obraia.backend.entity.phase.WorkSchedule;
import com.obraia.backend.entity.phase.WorkStatus;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.exception.ResourceNotFoundException;
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

    /** Deletes the phases and their tasks, only while nobody reported progress. */
    @Transactional
    public void reset(UUID projectId, UUID userId) {
        Project project = accessService.requireProject(projectId, userId);
        ProjectService.requireEditable(project);
        if (reportRepository.existsByTaskPhaseProjectId(projectId)) {
            throw new ConflictException("PHASES_HAVE_PROGRESS",
                    "The plan has progress reports, so it cannot be deleted");
        }
        phaseRepository.deleteByProjectId(projectId);
    }

    @Transactional
    public PhaseResponse update(UUID projectId, UUID userId, UUID phaseId, PhaseRequest request) {
        Project project = accessService.requireProject(projectId, userId);
        ProjectService.requireEditable(project);
        Phase phase = requirePhase(projectId, phaseId);
        phase.rename(request.name().strip());
        phase.getSchedule().plan(request.plannedStartDate(), request.plannedEndDate());
        phaseRepository.saveAndFlush(phase);
        return views.phase(phase);
    }

    /** A phase can only be completed when all its tasks are. */
    @Transactional
    public PhaseResponse changeStatus(UUID projectId, UUID userId, UUID phaseId, String statusCode) {
        Project project = accessService.requireProject(projectId, userId);
        ProjectService.requireEditable(project);
        Phase phase = requirePhase(projectId, phaseId);
        WorkStatus current = phase.getSchedule().getStatus();
        WorkStatus next = WorkStatus.fromCode(statusCode);
        if (!current.canChangeTo(next)) {
            throw new ConflictException("INVALID_STATUS_CHANGE", "A phase that is " + current.getCode()
                    + " cannot change to " + next.getCode() + ". Allowed: " + WorkProgress.allowed(current));
        }
        if (next == WorkStatus.COMPLETED && taskRepository.findByPhaseId(phaseId).stream()
                .anyMatch(task -> task.getSchedule().getStatus() != WorkStatus.COMPLETED)) {
            throw new ConflictException("PHASE_HAS_OPEN_TASKS",
                    "Complete every task of the phase before completing it");
        }
        phase.getSchedule().changeStatus(next, views.today());
        phaseRepository.saveAndFlush(phase);
        return views.phase(phase);
    }

    Phase requirePhase(UUID projectId, UUID phaseId) {
        return phaseRepository.findByIdAndProjectId(phaseId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PHASE_NOT_FOUND",
                        "The phase " + phaseId + " does not exist in this project"));
    }

    private List<PlannedPhase> plannedPhases(Project project) {
        LocalDate start = project.getPlannedStartDate();
        return factories.forType(project.getProjectType()).planPhases(
                templateRepository.findByProjectTypeIdAndSizeCategoryOrderBySequenceOrderAsc(
                        project.getProjectType().getId(), project.getSizeCategory()),
                project.getSizeCategory(), start);
    }
}
