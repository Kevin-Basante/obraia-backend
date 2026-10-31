package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.obraia.backend.dto.phase.ProgressReportRequest;
import com.obraia.backend.dto.phase.ProgressReportResponse;
import com.obraia.backend.entity.phase.Phase;
import com.obraia.backend.entity.phase.Task;
import com.obraia.backend.entity.phase.WorkStatus;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.project.SiteManager;
import com.obraia.backend.exception.FieldValidationException;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.PhaseRepository;
import com.obraia.backend.repository.ProgressReportRepository;
import com.obraia.backend.repository.TaskRepository;
import com.obraia.backend.repository.UserRepository;
import com.obraia.backend.support.PhaseFixtures;
import com.obraia.backend.support.ProjectFixtures;
import com.obraia.backend.support.TestUsers;

import jakarta.persistence.EntityManager;

class ProgressReportServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"),
            ZoneId.of("America/Bogota"));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    private final UUID userId = UUID.randomUUID();
    private final Project project = ProjectFixtures.project(userId, "Casa Avance");
    private final Phase phase = PhaseFixtures.phase(project, 1, "Cimentación");
    private final Task task = PhaseFixtures.task(phase, "Solado de limpieza");

    private ProgressReportRepository reports;
    private TaskRepository tasks;
    private ProgressReportService service;

    @BeforeEach
    void setUp() {
        reports = mock(ProgressReportRepository.class);
        tasks = mock(TaskRepository.class);
        PhaseRepository phases = mock(PhaseRepository.class);
        UserRepository users = mock(UserRepository.class);
        ProjectAccessService access = mock(ProjectAccessService.class);
        when(access.requireProject(project.getId(), userId)).thenReturn(project);
        when(tasks.findByIdAndPhaseProjectId(task.getId(), project.getId())).thenReturn(Optional.of(task));
        when(tasks.findByPhaseId(phase.getId())).thenReturn(List.of(task));
        when(reports.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        when(users.getReferenceById(userId)).thenReturn(TestUsers.verifiedUser("hash"));
        WorkPlanViews views = new WorkPlanViews(phases, tasks, reports, CLOCK);
        TaskService taskService = new TaskService(tasks, phases, access, views, CLOCK);
        service = new ProgressReportService(reports, users, taskService, access, views, mock(EntityManager.class));
    }

    private ProgressReportResponse report(String percentage) {
        return service.create(project.getId(), userId, task.getId(),
                new ProgressReportRequest(new BigDecimal(percentage), null, null, null));
    }

    @Test
    void firstProgressStartsTheTaskAndThePhase() {
        ProgressReportResponse response = report("40.00");

        assertEquals(TODAY, response.reportDate());
        assertEquals("Ana Perez", response.reportedBy());
        assertEquals(WorkStatus.IN_PROGRESS, task.getSchedule().getStatus());
        assertEquals(TODAY, task.getSchedule().getActualStartDate());
        assertEquals(WorkStatus.IN_PROGRESS, phase.getSchedule().getStatus());
    }

    @Test
    void zeroPercentLeavesTheTaskPlanned() {
        report("0");

        assertEquals(WorkStatus.PLANNED, task.getSchedule().getStatus());
        assertEquals(WorkStatus.PLANNED, phase.getSchedule().getStatus());
    }

    @Test
    void fullProgressCompletesTheTaskAndItsOnlyPhase() {
        report("100");

        assertEquals(WorkStatus.COMPLETED, task.getSchedule().getStatus());
        assertEquals(TODAY, task.getSchedule().getActualEndDate());
        assertEquals(WorkStatus.COMPLETED, phase.getSchedule().getStatus());
    }

    @Test
    void notesAreTrimmedAndBlankNotesDropped() {
        ProgressReportResponse withNotes = service.create(project.getId(), userId, task.getId(),
                new ProgressReportRequest(BigDecimal.TEN, "  Llovió en la tarde ", null, false));
        ProgressReportResponse blank = service.create(project.getId(), userId, task.getId(),
                new ProgressReportRequest(BigDecimal.TEN, "   ", null, false));

        assertEquals("Llovió en la tarde", withNotes.notes());
        assertNull(blank.notes());
    }

    @Test
    void futureDateIsRejected() {
        FieldValidationException ex = assertThrows(FieldValidationException.class,
                () -> service.create(project.getId(), userId, task.getId(),
                        new ProgressReportRequest(BigDecimal.TEN, null, TODAY.plusDays(1), false)));

        assertEquals(Map.of("reportDate", "cannot be in the future"), ex.getFields());
        verify(reports, never()).saveAndFlush(any());
    }

    @Test
    void pastDateIsKept() {
        ProgressReportResponse response = service.create(project.getId(), userId, task.getId(),
                new ProgressReportRequest(BigDecimal.TEN, null, TODAY.minusDays(3), false));

        assertEquals(TODAY.minusDays(3), response.reportDate());
    }

    @Test
    void siteManagerIsNeededToReportForThem() {
        FieldValidationException ex = assertThrows(FieldValidationException.class,
                () -> service.create(project.getId(), userId, task.getId(),
                        new ProgressReportRequest(BigDecimal.TEN, null, null, true)));

        assertEquals("the project has no site manager assigned", ex.getFields().get("fromSiteManager"));
    }

    @Test
    void reportFromTheAssignedSiteManager() {
        project.assignSiteManager(new SiteManager(userId, "Carlos Pérez", "1061234567", "3001234567"));

        ProgressReportResponse response = service.create(project.getId(), userId, task.getId(),
                new ProgressReportRequest(BigDecimal.TEN, null, null, true));

        assertEquals("Carlos Pérez", response.siteManager());
    }

    @Test
    void taskOfAnotherProjectIsNotFound() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.create(project.getId(), userId, UUID.randomUUID(),
                        new ProgressReportRequest(BigDecimal.TEN, null, null, false)));

        assertEquals("TASK_NOT_FOUND", ex.getCode());
    }
}
