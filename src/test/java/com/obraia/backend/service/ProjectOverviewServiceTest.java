package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import com.obraia.backend.dto.overview.MoneyOverview;
import com.obraia.backend.dto.overview.PortfolioResponse;
import com.obraia.backend.dto.overview.ProjectCard;
import com.obraia.backend.dto.overview.ProjectOverviewResponse;
import com.obraia.backend.entity.blueprint.BlueprintMeasurement;
import com.obraia.backend.entity.blueprint.MeasurementSource;
import com.obraia.backend.entity.budget.BudgetStatus;
import com.obraia.backend.entity.catalog.ElementType;
import com.obraia.backend.entity.expense.ExpenseCategory;
import com.obraia.backend.entity.phase.Phase;
import com.obraia.backend.entity.phase.Task;
import com.obraia.backend.entity.phase.WorkStatus;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.project.ProjectStatus;
import com.obraia.backend.entity.risk.RiskAssessment;
import com.obraia.backend.entity.risk.RiskLevel;
import com.obraia.backend.entity.risk.RiskTrigger;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.BlueprintMeasurementRepository;
import com.obraia.backend.repository.BlueprintRepository;
import com.obraia.backend.repository.BudgetRepository;
import com.obraia.backend.repository.ExpenseCategoryTotal;
import com.obraia.backend.repository.ExpenseRepository;
import com.obraia.backend.repository.PhaseRepository;
import com.obraia.backend.repository.ProgressReportRepository;
import com.obraia.backend.repository.ProjectMemberRepository;
import com.obraia.backend.repository.ProjectRepository;
import com.obraia.backend.repository.RiskAssessmentRepository;
import com.obraia.backend.repository.TaskRepository;
import com.obraia.backend.support.BlueprintFixtures;
import com.obraia.backend.support.BudgetFixtures;
import com.obraia.backend.support.PhaseFixtures;
import com.obraia.backend.support.ProjectFixtures;
import com.obraia.backend.support.TestUsers;

class ProjectOverviewServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"),
            ZoneId.of("America/Bogota"));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 20);

    private final UUID userId = UUID.randomUUID();
    private final Project house = ProjectFixtures.project(userId, "Casa Resumen", ProjectStatus.IN_PROGRESS);
    private final Project shop = ProjectFixtures.project(userId, "Local Resumen");
    private final Phase foundation = PhaseFixtures.phase(house, 1, "Cimentación");
    private final Task digging = PhaseFixtures.task(foundation, "Excavación");
    private final Task footings = PhaseFixtures.task(foundation, "Zapatas");

    private ProjectAccessService access;
    private ProjectRepository projects;
    private BudgetRepository budgets;
    private ExpenseRepository expenses;
    private RiskAssessmentRepository risks;
    private ProjectOverviewService service;

    @BeforeEach
    void setUp() {
        access = mock(ProjectAccessService.class);
        projects = mock(ProjectRepository.class);
        PhaseRepository phases = mock(PhaseRepository.class);
        TaskRepository tasks = mock(TaskRepository.class);
        ProgressReportRepository reports = mock(ProgressReportRepository.class);
        budgets = mock(BudgetRepository.class);
        expenses = mock(ExpenseRepository.class);
        risks = mock(RiskAssessmentRepository.class);
        BlueprintRepository blueprints = mock(BlueprintRepository.class);
        BlueprintMeasurementRepository measurements = mock(BlueprintMeasurementRepository.class);
        ProjectMemberRepository members = mock(ProjectMemberRepository.class);

        digging.getSchedule().plan(TODAY.minusDays(10), TODAY.minusDays(5));
        footings.getSchedule().plan(TODAY.minusDays(4), TODAY.plusDays(3));
        digging.getSchedule().changeStatus(WorkStatus.IN_PROGRESS, TODAY.minusDays(10));
        digging.getSchedule().changeStatus(WorkStatus.COMPLETED, TODAY.minusDays(5));
        when(phases.findByProjectIdOrderBySequenceOrderAsc(house.getId())).thenReturn(List.of(foundation));
        when(tasks.findByPhaseProjectIdOrderByCreatedAtAsc(house.getId())).thenReturn(List.of(digging, footings));
        when(reports.findByTaskPhaseProjectIdOrderByReportDateDescCreatedAtDesc(house.getId())).thenReturn(
                List.of(PhaseFixtures.report(footings, "50.00", TODAY.minusDays(1))));
        when(budgets.findByProjectIdOrderByVersionDesc(house.getId())).thenReturn(List.of(
                BudgetFixtures.budget(house, 2, "1200000.00"), BudgetFixtures.approved(house, 1, "1000000.00")));
        when(budgets.findByProjectIdAndStatus(house.getId(), BudgetStatus.APPROVED))
                .thenReturn(Optional.of(BudgetFixtures.approved(house, 1, "1000000.00")));
        when(expenses.sumByProjectId(house.getId())).thenReturn(new BigDecimal("800000.00"));
        when(expenses.sumByProjectId(shop.getId())).thenReturn(BigDecimal.ZERO);
        when(expenses.totalsByCategory(house.getId())).thenReturn(List.of(
                new ExpenseCategoryTotal(ExpenseCategory.LABOR, new BigDecimal("300000.00"), 1L),
                new ExpenseCategoryTotal(ExpenseCategory.MATERIALS, new BigDecimal("500000.00"), 2L)));
        when(risks.findFirstByProjectIdOrderByAssessedAtDesc(house.getId())).thenReturn(Optional.of(
                new RiskAssessment(house, RiskLevel.HIGH, new BigDecimal("61.50"), 3, "[]", "rules-v1",
                        RiskTrigger.EXPENSE, CLOCK.instant())));
        when(blueprints.countByProjectId(house.getId())).thenReturn(1L);
        when(measurements.findByProjectIdOrderByCreatedAtAsc(house.getId())).thenReturn(List.of(
                wall(MeasurementSource.MANUAL, "10"), wall(MeasurementSource.MANUAL, "6"),
                wall(MeasurementSource.OCR, "16")));
        when(members.findByProjectIdOrderByJoinedAtAsc(house.getId())).thenReturn(List.of(
                ProjectFixtures.member(house, TestUsers.verifiedUser("hash"))));
        when(projects.findVisibleTo(userId)).thenReturn(List.of(shop, house));

        WorkPlanViews views = new WorkPlanViews(phases, tasks, reports, CLOCK);
        service = new ProjectOverviewService(access, projects, views, budgets, expenses, risks, blueprints,
                measurements, members);
    }

    @Test
    void overviewGathersThePlanTheMoneyTheRiskAndTheCounts() {
        ProjectOverviewResponse overview = service.overview(house.getId(), userId);

        assertEquals(house.getId(), overview.projectId());
        assertEquals(new BigDecimal("75.00"), overview.work().progressPct());
        assertEquals(1, overview.work().completedTasks());
        assertEquals(List.of("Zapatas"), overview.work().upcomingTasks().stream().map(t -> t.name()).toList());
        assertEquals("high", overview.risk().level().code());
        assertEquals(new BigDecimal("61.50"), overview.risk().score());
        assertEquals(1, overview.counts().blueprints());
        assertEquals(2, overview.counts().manualMeasurements());
        assertEquals(1, overview.counts().aiMeasurements());
        assertEquals(1, overview.counts().members());
    }

    @Test
    void moneyComparesTheApprovedVersionWithTheExpenses() {
        MoneyOverview money = service.overview(house.getId(), userId).money();

        assertEquals(1, money.approvedVersion());
        assertEquals(2, money.budgetVersions());
        assertEquals(new BigDecimal("1000000.00"), money.approvedBudget());
        assertEquals(new BigDecimal("800000.00"), money.totalSpent());
        assertEquals(new BigDecimal("200000.00"), money.remaining());
        assertEquals(new BigDecimal("80.00"), money.spentPct());
        assertEquals(3, money.expenseCount());
        // Biggest category first.
        assertEquals("materials", money.byCategory().get(0).category().code());
    }

    @Test
    void moneyWithoutAnApprovedBudgetHasNoComparison() {
        when(budgets.findByProjectIdOrderByVersionDesc(house.getId()))
                .thenReturn(List.of(BudgetFixtures.budget(house, 1, "1000000.00")));

        MoneyOverview money = service.overview(house.getId(), userId).money();

        assertNull(money.approvedVersion());
        assertEquals(1, money.budgetVersions());
        assertNull(money.approvedBudget());
        assertNull(money.remaining());
        assertNull(money.spentPct());
        assertEquals(new BigDecimal("800000.00"), money.totalSpent());
    }

    @Test
    void overviewBeforeTheFirstAssessmentHasNoRisk() {
        when(risks.findFirstByProjectIdOrderByAssessedAtDesc(house.getId())).thenReturn(Optional.empty());

        assertNull(service.overview(house.getId(), userId).risk());
    }

    @Test
    void overviewOfAProjectOfSomeoneElseIsNotFound() {
        UUID stranger = UUID.randomUUID();
        when(access.requireMembership(house.getId(), stranger))
                .thenThrow(new ResourceNotFoundException("PROJECT_NOT_FOUND", "The project does not exist"));

        assertThrows(ResourceNotFoundException.class, () -> service.overview(house.getId(), stranger));
    }

    @Test
    void portfolioAddsUpEveryProject() {
        PortfolioResponse portfolio = service.portfolio(userId);

        assertEquals(2, portfolio.projectCount());
        assertEquals(1, portfolio.active());
        assertEquals(1, portfolio.atRisk());
        assertEquals(new BigDecimal("1000000.00"), portfolio.approvedBudget());
        assertEquals(new BigDecimal("800000.00"), portfolio.totalSpent());
        assertEquals(List.of("Local Resumen", "Casa Resumen"),
                portfolio.projects().stream().map(card -> card.project().name()).toList());
    }

    @Test
    void projectCardShowsProgressMoneyAndRisk() {
        ProjectCard house = service.portfolio(userId).projects().get(1);
        ProjectCard shop = service.portfolio(userId).projects().get(0);

        assertEquals(new BigDecimal("75.00"), house.progressPct());
        assertEquals(new BigDecimal("80.00"), house.spentPct());
        assertEquals("high", house.risk().level().code());
        assertEquals(new BigDecimal("0.00"), shop.progressPct());
        assertNull(shop.approvedBudget());
        assertNull(shop.spentPct());
        assertNull(shop.risk());
    }

    @Test
    void portfolioWithoutProjectsIsEmpty() {
        when(projects.findVisibleTo(userId)).thenReturn(List.of());

        PortfolioResponse portfolio = service.portfolio(userId);

        assertEquals(0, portfolio.projectCount());
        assertEquals(BigDecimal.ZERO, portfolio.approvedBudget());
        assertEquals(BigDecimal.ZERO, portfolio.totalSpent());
    }

    private BlueprintMeasurement wall(MeasurementSource source, String lengthM) {
        return BlueprintFixtures.measurement(house, source, ElementType.WALL, 1, lengthM, null, "2.4", 1);
    }
}
