package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
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

import com.obraia.backend.entity.budget.BudgetStatus;
import com.obraia.backend.entity.phase.Phase;
import com.obraia.backend.entity.phase.Task;
import com.obraia.backend.entity.phase.WorkStatus;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.repository.BudgetRepository;
import com.obraia.backend.repository.ExpenseRepository;
import com.obraia.backend.repository.PhaseRepository;
import com.obraia.backend.repository.ProgressReportRepository;
import com.obraia.backend.repository.TaskRepository;
import com.obraia.backend.risk.ProjectSnapshot;
import com.obraia.backend.support.BudgetFixtures;
import com.obraia.backend.support.PhaseFixtures;
import com.obraia.backend.support.ProjectFixtures;

class ProjectSnapshotsTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"),
            ZoneId.of("America/Bogota"));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 20);

    private final Project project = ProjectFixtures.project(UUID.randomUUID(), "Casa Riesgo");
    private final Phase foundation = PhaseFixtures.phase(project, 1, "Cimentación");
    private final Phase walls = PhaseFixtures.phase(project, 2, "Muros");
    private final Task digging = PhaseFixtures.task(foundation, "Excavación");
    private final Task footings = PhaseFixtures.task(foundation, "Zapatas");

    private BudgetRepository budgets;
    private ExpenseRepository expenses;
    private ProjectSnapshots snapshots;

    @BeforeEach
    void setUp() {
        PhaseRepository phases = mock(PhaseRepository.class);
        TaskRepository tasks = mock(TaskRepository.class);
        ProgressReportRepository reports = mock(ProgressReportRepository.class);
        budgets = mock(BudgetRepository.class);
        expenses = mock(ExpenseRepository.class);
        foundation.getSchedule().plan(TODAY.minusDays(20), TODAY.minusDays(5));
        walls.getSchedule().plan(TODAY.minusDays(4), TODAY.minusDays(1));
        digging.getSchedule().plan(TODAY.minusDays(20), TODAY.minusDays(10));
        footings.getSchedule().plan(TODAY.minusDays(9), TODAY.minusDays(5));
        digging.getSchedule().changeStatus(WorkStatus.IN_PROGRESS, TODAY.minusDays(20));
        digging.getSchedule().changeStatus(WorkStatus.COMPLETED, TODAY.minusDays(10));
        when(phases.findByProjectIdOrderBySequenceOrderAsc(project.getId())).thenReturn(List.of(foundation, walls));
        when(tasks.findByPhaseProjectIdOrderByCreatedAtAsc(project.getId())).thenReturn(List.of(digging, footings));
        when(reports.findByTaskPhaseProjectIdOrderByReportDateDescCreatedAtDesc(project.getId())).thenReturn(
                List.of(PhaseFixtures.report(digging, "100.00", TODAY.minusDays(10)),
                        PhaseFixtures.report(footings, "50.00", TODAY.minusDays(6))));
        when(budgets.findByProjectIdAndStatus(project.getId(), BudgetStatus.APPROVED)).thenReturn(Optional.empty());
        when(expenses.sumByProjectId(project.getId())).thenReturn(new BigDecimal("300000.00"));
        snapshots = new ProjectSnapshots(new WorkPlanViews(phases, tasks, reports, CLOCK), budgets, expenses);
    }

    @Test
    void readsThePlanTheLateWorkAndTheMoney() {
        ProjectSnapshot snapshot = snapshots.read(project.getId());

        assertEquals(TODAY, snapshot.today());
        assertEquals(TODAY.minusDays(20), snapshot.plannedStart());
        assertEquals(TODAY.minusDays(1), snapshot.plannedEnd());
        assertEquals(new BigDecimal("75.00"), snapshot.progressPct());
        // The completed task is not late; the walls phase has no tasks, so it counts itself.
        assertEquals(List.of(new ProjectSnapshot.LateWork("Zapatas", 5), new ProjectSnapshot.LateWork("Muros", 1)),
                snapshot.lateWork());
        assertNull(snapshot.approvedBudget());
        assertEquals(new BigDecimal("300000.00"), snapshot.spent());
    }

    @Test
    void readsTheApprovedBudget() {
        when(budgets.findByProjectIdAndStatus(project.getId(), BudgetStatus.APPROVED))
                .thenReturn(Optional.of(BudgetFixtures.approved(project, 1, "2000000.00")));

        assertEquals(new BigDecimal("2000000.00"), snapshots.read(project.getId()).approvedBudget());
    }
}
