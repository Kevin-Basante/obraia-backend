package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.obraia.backend.dto.phase.TaskRequest;
import com.obraia.backend.dto.phase.TaskResponse;
import com.obraia.backend.entity.phase.Phase;
import com.obraia.backend.entity.phase.Task;
import com.obraia.backend.entity.phase.WorkStatus;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.exception.FieldValidationException;
import com.obraia.backend.repository.PhaseRepository;
import com.obraia.backend.repository.ProgressReportRepository;
import com.obraia.backend.repository.TaskRepository;
import com.obraia.backend.support.PhaseFixtures;
import com.obraia.backend.support.ProjectFixtures;

class TaskServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"),
            ZoneId.of("America/Bogota"));

    private final UUID userId = UUID.randomUUID();
    private final Project project = ProjectFixtures.project(userId, "Casa Tareas");
    private final Phase foundation = PhaseFixtures.phase(project, 1, "Cimentación");
    private final Phase masonry = PhaseFixtures.phase(project, 2, "Mampostería");

    private TaskRepository tasks;
    private TaskService service;

    @BeforeEach
    void setUp() {
        tasks = mock(TaskRepository.class);
        PhaseRepository phases = mock(PhaseRepository.class);
        ProjectAccessService access = mock(ProjectAccessService.class);
        when(access.requireProject(project.getId(), userId)).thenReturn(project);
        when(phases.findByIdAndProjectId(foundation.getId(), project.getId())).thenReturn(Optional.of(foundation));
        when(phases.findByIdAndProjectId(masonry.getId(), project.getId())).thenReturn(Optional.of(masonry));
        when(tasks.save(any())).thenAnswer(call -> call.getArgument(0));
        WorkPlanViews views = new WorkPlanViews(phases, tasks, mock(ProgressReportRepository.class), CLOCK);
        service = new TaskService(tasks, phases, access, views, CLOCK);
    }

    @Test
    void taskIsAddedToItsPhase() {
        TaskResponse response = service.create(project.getId(), userId,
                new TaskRequest(foundation.getId(), "  Revisar niveles ", null, LocalDate.of(2026, 11, 12)));

        assertEquals(foundation.getId(), response.phaseId());
        assertEquals("Revisar niveles", response.name());
        assertEquals("planned", response.status().code());
        assertEquals(List.of("in_progress"), response.nextStatuses().stream().map(s -> s.code()).toList());
    }

    @Test
    void phaseOfAnotherProjectIsAFieldError() {
        FieldValidationException ex = assertThrows(FieldValidationException.class, () -> service.create(
                project.getId(), userId, new TaskRequest(UUID.randomUUID(), "Revisar niveles", null, null)));

        assertEquals("the phase does not belong to this project", ex.getFields().get("phaseId"));
        verify(tasks, never()).save(any());
    }

    @Test
    void newTaskReopensACompletedPhase() {
        foundation.getSchedule().changeStatus(WorkStatus.IN_PROGRESS, LocalDate.of(2026, 11, 2));
        foundation.getSchedule().changeStatus(WorkStatus.COMPLETED, LocalDate.of(2026, 11, 9));
        Task done = PhaseFixtures.task(foundation, "Solado");
        done.getSchedule().changeStatus(WorkStatus.IN_PROGRESS, LocalDate.of(2026, 11, 2));
        done.getSchedule().changeStatus(WorkStatus.COMPLETED, LocalDate.of(2026, 11, 9));
        Task added = PhaseFixtures.task(foundation, "Revisar niveles");
        when(tasks.findByPhaseId(foundation.getId())).thenReturn(List.of(done, added));

        service.create(project.getId(), userId, new TaskRequest(foundation.getId(), "Revisar niveles", null, null));

        assertEquals(WorkStatus.IN_PROGRESS, foundation.getSchedule().getStatus());
    }

    @Test
    void movingATaskUpdatesBothPhases() {
        Task task = PhaseFixtures.task(foundation, "Solado");
        task.getSchedule().changeStatus(WorkStatus.IN_PROGRESS, LocalDate.of(2026, 11, 9));
        when(tasks.findByIdAndPhaseProjectId(task.getId(), project.getId())).thenReturn(Optional.of(task));
        when(tasks.findByPhaseId(masonry.getId())).thenReturn(List.of(task));

        TaskResponse response = service.update(project.getId(), userId, task.getId(),
                new TaskRequest(masonry.getId(), "Solado", null, null));

        assertEquals(masonry.getId(), response.phaseId());
        assertEquals(WorkStatus.IN_PROGRESS, masonry.getSchedule().getStatus());
        verify(tasks).findByPhaseId(foundation.getId());
    }

    @Test
    void completingTheLastTaskCompletesThePhase() {
        Task task = PhaseFixtures.task(foundation, "Solado");
        task.getSchedule().changeStatus(WorkStatus.IN_PROGRESS, LocalDate.of(2026, 11, 9));
        when(tasks.findByIdAndPhaseProjectId(task.getId(), project.getId())).thenReturn(Optional.of(task));
        when(tasks.findByPhaseId(foundation.getId())).thenReturn(List.of(task));

        TaskResponse response = service.changeStatus(project.getId(), userId, task.getId(), "completed");

        assertEquals(LocalDate.of(2026, 11, 10), response.actualEndDate());
        assertEquals(WorkStatus.COMPLETED, foundation.getSchedule().getStatus());
    }

    @Test
    void plannedTaskCannotBeCompletedDirectly() {
        Task task = PhaseFixtures.task(foundation, "Solado");
        when(tasks.findByIdAndPhaseProjectId(task.getId(), project.getId())).thenReturn(Optional.of(task));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.changeStatus(project.getId(), userId, task.getId(), "completed"));

        assertEquals("INVALID_STATUS_CHANGE", ex.getCode());
        assertEquals("A task that is planned cannot change to completed. Allowed: in_progress", ex.getMessage());
    }

    @Test
    void deletingATaskUpdatesItsPhase() {
        Task task = PhaseFixtures.task(foundation, "Solado");
        when(tasks.findByIdAndPhaseProjectId(task.getId(), project.getId())).thenReturn(Optional.of(task));

        service.delete(project.getId(), userId, task.getId());

        verify(tasks).delete(task);
        verify(tasks).findByPhaseId(foundation.getId());
    }
}
