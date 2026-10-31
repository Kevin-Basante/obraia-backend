package com.obraia.backend.service;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.phase.TaskRequest;
import com.obraia.backend.dto.phase.TaskResponse;
import com.obraia.backend.entity.phase.Phase;
import com.obraia.backend.entity.phase.Task;
import com.obraia.backend.entity.phase.WorkSchedule;
import com.obraia.backend.entity.phase.WorkStatus;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.exception.FieldValidationException;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.PhaseRepository;
import com.obraia.backend.repository.TaskRepository;

/** Tasks of the phases. Every change keeps the status of the phase in step. */
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final PhaseRepository phaseRepository;
    private final ProjectAccessService accessService;
    private final WorkPlanViews views;
    private final Clock clock;

    public TaskService(TaskRepository taskRepository, PhaseRepository phaseRepository,
            ProjectAccessService accessService, WorkPlanViews views, Clock clock) {
        this.taskRepository = taskRepository;
        this.phaseRepository = phaseRepository;
        this.accessService = accessService;
        this.views = views;
        this.clock = clock;
    }

    @Transactional
    public TaskResponse create(UUID projectId, UUID userId, TaskRequest request) {
        Project project = accessService.requireProject(projectId, userId);
        ProjectService.requireEditable(project);
        Phase phase = requirePhaseField(projectId, request.phaseId());
        Task task = taskRepository.save(new Task(phase, request.name().strip(),
                new WorkSchedule(request.plannedStartDate(), request.plannedEndDate()), clock.instant()));
        syncPhase(phase);
        taskRepository.flush();
        return views.task(task);
    }

    /** A different phaseId moves the task to that phase. */
    @Transactional
    public TaskResponse update(UUID projectId, UUID userId, UUID taskId, TaskRequest request) {
        Project project = accessService.requireProject(projectId, userId);
        ProjectService.requireEditable(project);
        Task task = requireTask(projectId, taskId);
        Phase previous = task.getPhase();
        Phase phase = requirePhaseField(projectId, request.phaseId());
        task.update(phase, request.name().strip());
        task.getSchedule().plan(request.plannedStartDate(), request.plannedEndDate());
        taskRepository.flush();
        syncPhase(phase);
        if (!previous.getId().equals(phase.getId())) {
            syncPhase(previous);
        }
        taskRepository.flush();
        return views.task(task);
    }

    /** Its progress reports are deleted with it. */
    @Transactional
    public void delete(UUID projectId, UUID userId, UUID taskId) {
        Project project = accessService.requireProject(projectId, userId);
        ProjectService.requireEditable(project);
        Task task = requireTask(projectId, taskId);
        Phase phase = task.getPhase();
        taskRepository.delete(task);
        taskRepository.flush();
        syncPhase(phase);
    }

    @Transactional
    public TaskResponse changeStatus(UUID projectId, UUID userId, UUID taskId, String statusCode) {
        Project project = accessService.requireProject(projectId, userId);
        ProjectService.requireEditable(project);
        Task task = requireTask(projectId, taskId);
        WorkStatus current = task.getSchedule().getStatus();
        WorkStatus next = WorkStatus.fromCode(statusCode);
        if (!current.canChangeTo(next)) {
            throw new ConflictException("INVALID_STATUS_CHANGE", "A task that is " + current.getCode()
                    + " cannot change to " + next.getCode() + ". Allowed: " + WorkProgress.allowed(current));
        }
        task.getSchedule().changeStatus(next, views.today());
        taskRepository.flush();
        syncPhase(task.getPhase());
        taskRepository.flush();
        return views.task(task);
    }

    Task requireTask(UUID projectId, UUID taskId) {
        return taskRepository.findByIdAndPhaseProjectId(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("TASK_NOT_FOUND",
                        "The task " + taskId + " does not exist in this project"));
    }

    void syncPhase(Phase phase) {
        WorkProgress.syncPhase(phase, taskRepository.findByPhaseId(phase.getId()), views.today());
        phaseRepository.save(phase);
    }

    /** The phase comes in the body, so a wrong one is a field error. */
    private Phase requirePhaseField(UUID projectId, UUID phaseId) {
        return phaseRepository.findByIdAndProjectId(phaseId, projectId)
                .orElseThrow(() -> new FieldValidationException(
                        Map.of("phaseId", "the phase does not belong to this project")));
    }
}
