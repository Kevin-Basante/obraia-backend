package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.obraia.backend.dto.phase.PhaseRequest;
import com.obraia.backend.dto.phase.PhaseResponse;
import com.obraia.backend.dto.phase.PlanPreviewResponse;
import com.obraia.backend.dto.phase.WorkPlanResponse;
import com.obraia.backend.entity.catalog.SizeCategory;
import com.obraia.backend.entity.phase.Phase;
import com.obraia.backend.entity.phase.Task;
import com.obraia.backend.entity.phase.WorkStatus;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.project.ProjectStatus;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.planning.CommercialUnitCalculatorFactory;
import com.obraia.backend.planning.HouseCalculatorFactory;
import com.obraia.backend.planning.ProjectCalculatorFactories;
import com.obraia.backend.repository.PhaseRepository;
import com.obraia.backend.repository.PhaseTemplateRepository;
import com.obraia.backend.repository.ProgressReportRepository;
import com.obraia.backend.repository.TaskRepository;
import com.obraia.backend.support.CatalogFixtures;
import com.obraia.backend.support.PhaseFixtures;
import com.obraia.backend.support.ProjectFixtures;

class PhaseServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"),
            ZoneId.of("America/Bogota"));

    private final UUID userId = UUID.randomUUID();
    // 90 m2 house, so a small one (see ProjectFixtures).
    private final Project project = ProjectFixtures.project(userId, "Casa Fases");

    private PhaseRepository phases;
    private TaskRepository tasks;
    private ProgressReportRepository reports;
    private PhaseTemplateRepository templates;
    private ProjectAccessService access;
    private PhaseService service;

    @BeforeEach
    void setUp() {
        phases = mock(PhaseRepository.class);
        tasks = mock(TaskRepository.class);
        reports = mock(ProgressReportRepository.class);
        templates = mock(PhaseTemplateRepository.class);
        access = mock(ProjectAccessService.class);
        when(access.requireProject(project.getId(), userId)).thenReturn(project);
        when(phases.save(any())).thenAnswer(call -> call.getArgument(0));
        when(templates.findByProjectTypeIdAndSizeCategoryOrderBySequenceOrderAsc(project.getProjectType().getId(),
                SizeCategory.SMALL)).thenReturn(List.of(
                        CatalogFixtures.phase(project.getProjectType(), SizeCategory.SMALL, 1,
                                "Preliminares y replanteo", 5),
                        CatalogFixtures.phase(project.getProjectType(), SizeCategory.SMALL, 2, "Mampostería", 21)));
        ProjectCalculatorFactories factories = new ProjectCalculatorFactories(
                List.of(new HouseCalculatorFactory(), new CommercialUnitCalculatorFactory()));
        WorkPlanViews views = new WorkPlanViews(phases, tasks, reports, CLOCK);
        service = new PhaseService(phases, tasks, reports, templates, factories, access, views, CLOCK);
    }

    @Test
    void previewUsesThePlannerOfTheProjectType() {
        PlanPreviewResponse preview = service.preview(project.getId(), userId);

        assertEquals("Vivienda unifamiliar", preview.projectType());
        assertEquals("small", preview.sizeCategory());
        assertEquals(26, preview.workingDays());
        assertEquals(List.of("Muros de fachada", "Muros interiores"), preview.phases().get(1).tasks());
        verify(phases, never()).save(any());
    }

    @Test
    void generateSavesThePhasesWithTheirTasksInOrder() {
        service.generate(project.getId(), userId);

        ArgumentCaptor<Phase> savedPhases = ArgumentCaptor.forClass(Phase.class);
        verify(phases, times(2)).save(savedPhases.capture());
        assertEquals(List.of("Preliminares y replanteo", "Mampostería"),
                savedPhases.getAllValues().stream().map(Phase::getName).toList());
        ArgumentCaptor<Task> savedTasks = ArgumentCaptor.forClass(Task.class);
        verify(tasks, times(5)).save(savedTasks.capture());
        List<Task> created = savedTasks.getAllValues();
        assertEquals("Limpieza del lote", created.get(0).getName());
        assertEquals(CLOCK.instant(), created.get(0).getCreatedAt());
        // One microsecond apart, so they are read back in the same order.
        assertEquals(CLOCK.instant().plusNanos(4_000), created.get(4).getCreatedAt());
    }

    @Test
    void planIsCreatedOnlyOnce() {
        when(phases.existsByProjectId(project.getId())).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.generate(project.getId(), userId));

        assertEquals("PHASES_ALREADY_CREATED", ex.getCode());
        verify(phases, never()).save(any());
    }

    @Test
    void closedProjectGetsNoPlan() {
        Project closed = ProjectFixtures.project(userId, "Casa cerrada", ProjectStatus.COMPLETED);
        when(access.requireProject(closed.getId(), userId)).thenReturn(closed);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.generate(closed.getId(), userId));

        assertEquals("PROJECT_CLOSED", ex.getCode());
    }

    @Test
    void planWithProgressCannotBeDeleted() {
        when(reports.existsByTaskPhaseProjectId(project.getId())).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.reset(project.getId(), userId));

        assertEquals("PHASES_HAVE_PROGRESS", ex.getCode());
        verify(phases, never()).deleteByProjectId(any());
    }

    @Test
    void planWithoutProgressIsDeleted() {
        service.reset(project.getId(), userId);

        verify(phases).deleteByProjectId(project.getId());
    }

    @Test
    void progressComesFromTheLatestReportOfEachTask() {
        Phase phase = PhaseFixtures.phase(project, 1, "Cimentación");
        Task reported = PhaseFixtures.task(phase, "Solado de limpieza");
        Task pending = PhaseFixtures.task(phase, "Fundida de zapatas");
        when(phases.findByProjectIdOrderBySequenceOrderAsc(project.getId())).thenReturn(List.of(phase));
        when(tasks.findByPhaseProjectIdOrderByCreatedAtAsc(project.getId())).thenReturn(List.of(reported, pending));
        when(reports.findByTaskPhaseProjectIdOrderByReportDateDescCreatedAtDesc(project.getId())).thenReturn(List.of(
                PhaseFixtures.report(reported, "60.00", LocalDate.of(2026, 11, 9)),
                PhaseFixtures.report(reported, "20.00", LocalDate.of(2026, 11, 5))));

        WorkPlanResponse plan = service.plan(project.getId(), userId);

        PhaseResponse response = plan.phases().get(0);
        assertEquals(new BigDecimal("60.00"), response.tasks().get(0).progressPct());
        assertEquals(2, response.tasks().get(0).reportCount());
        assertEquals(new BigDecimal("0.00"), response.tasks().get(1).progressPct());
        assertEquals(new BigDecimal("30.00"), response.progressPct());
        assertEquals(new BigDecimal("30.00"), plan.progressPct());
    }

    @Test
    void phaseIsRenamedAndRescheduled() {
        Phase phase = PhaseFixtures.phase(project, 1, "Cimentación");
        when(phases.findByIdAndProjectId(phase.getId(), project.getId())).thenReturn(Optional.of(phase));

        PhaseResponse response = service.update(project.getId(), userId, phase.getId(),
                new PhaseRequest("  Cimentación ajustada ", LocalDate.of(2026, 11, 16), LocalDate.of(2026, 12, 1)));

        assertEquals("Cimentación ajustada", response.name());
        assertEquals(LocalDate.of(2026, 12, 1), response.plannedEndDate());
        assertFalse(response.late());
    }

    @Test
    void unknownPhaseIsNotFound() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.changeStatus(project.getId(), userId, UUID.randomUUID(), "in_progress"));

        assertEquals("PHASE_NOT_FOUND", ex.getCode());
    }

    @Test
    void phaseWithOpenTasksCannotBeCompleted() {
        Phase phase = PhaseFixtures.phase(project, 1, "Cimentación");
        phase.getSchedule().changeStatus(WorkStatus.IN_PROGRESS, LocalDate.now(CLOCK));
        when(phases.findByIdAndProjectId(phase.getId(), project.getId())).thenReturn(Optional.of(phase));
        when(tasks.findByPhaseId(phase.getId())).thenReturn(List.of(PhaseFixtures.task(phase, "Solado")));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.changeStatus(project.getId(), userId, phase.getId(), "completed"));

        assertEquals("PHASE_HAS_OPEN_TASKS", ex.getCode());
    }

    @Test
    void plannedPhaseCannotJumpToCompleted() {
        Phase phase = PhaseFixtures.phase(project, 1, "Cimentación");
        when(phases.findByIdAndProjectId(phase.getId(), project.getId())).thenReturn(Optional.of(phase));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.changeStatus(project.getId(), userId, phase.getId(), "completed"));

        assertEquals("INVALID_STATUS_CHANGE", ex.getCode());
    }

    @Test
    void phaseStartsWithTodayAsRealStart() {
        Phase phase = PhaseFixtures.phase(project, 1, "Cimentación");
        when(phases.findByIdAndProjectId(phase.getId(), project.getId())).thenReturn(Optional.of(phase));

        PhaseResponse response = service.changeStatus(project.getId(), userId, phase.getId(), "in_progress");

        assertEquals("in_progress", response.status().code());
        assertEquals(LocalDate.of(2026, 11, 10), response.actualStartDate());
    }
}
