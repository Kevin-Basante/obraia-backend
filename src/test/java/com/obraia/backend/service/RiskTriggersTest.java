package com.obraia.backend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.project.ProjectStatus;
import com.obraia.backend.entity.risk.RiskTrigger;
import com.obraia.backend.event.ExpensesChangedEvent;
import com.obraia.backend.event.ProgressReportedEvent;
import com.obraia.backend.repository.ProjectRepository;
import com.obraia.backend.support.ProjectFixtures;

class RiskTriggersTest {

    private final UUID userId = UUID.randomUUID();

    private ProjectRepository projects;
    private RiskAssessmentService riskService;
    private RiskTriggers triggers;

    @BeforeEach
    void setUp() {
        projects = mock(ProjectRepository.class);
        riskService = mock(RiskAssessmentService.class);
        triggers = new RiskTriggers(projects, riskService, mock(PlatformTransactionManager.class));
    }

    @Test
    void anExpenseAssessesTheRisk() {
        Project project = ProjectFixtures.project(userId, "Casa Gastos", ProjectStatus.IN_PROGRESS);
        when(projects.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));

        triggers.onExpensesChanged(new ExpensesChangedEvent(project.getId()));

        verify(riskService).assessAutomatically(project, RiskTrigger.EXPENSE);
    }

    @Test
    void aProgressReportAssessesTheRisk() {
        Project project = ProjectFixtures.project(userId, "Casa Avance", ProjectStatus.IN_PROGRESS);
        when(projects.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));

        triggers.onProgressReported(new ProgressReportedEvent(project.getId()));

        verify(riskService).assessAutomatically(project, RiskTrigger.PROGRESS_REPORT);
    }

    @Test
    void closedAndDeletedProjectsAreSkipped() {
        Project closed = ProjectFixtures.project(userId, "Casa Cerrada", ProjectStatus.COMPLETED);
        when(projects.findByIdAndDeletedAtIsNull(closed.getId())).thenReturn(Optional.of(closed));

        triggers.onExpensesChanged(new ExpensesChangedEvent(closed.getId()));
        triggers.onExpensesChanged(new ExpensesChangedEvent(UUID.randomUUID()));

        verify(riskService, never()).assessAutomatically(any(), any());
    }

    @Test
    void aFailureIsLoggedAndNotThrown() {
        Project project = ProjectFixtures.project(userId, "Casa Falla", ProjectStatus.IN_PROGRESS);
        when(projects.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
        doThrow(new IllegalStateException("boom")).when(riskService).assessAutomatically(any(), any());

        triggers.onExpensesChanged(new ExpensesChangedEvent(project.getId()));

        verify(riskService).assessAutomatically(project, RiskTrigger.EXPENSE);
    }
}
