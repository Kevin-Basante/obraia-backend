package com.obraia.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.expense.ExpenseSummaryResponse;
import com.obraia.backend.dto.overview.ActivityCounts;
import com.obraia.backend.dto.overview.MoneyOverview;
import com.obraia.backend.dto.overview.ProjectOverviewResponse;
import com.obraia.backend.dto.overview.RiskBadge;
import com.obraia.backend.dto.overview.WorkOverviews;
import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.entity.blueprint.BlueprintMeasurement;
import com.obraia.backend.entity.blueprint.MeasurementSource;
import com.obraia.backend.entity.budget.Budget;
import com.obraia.backend.entity.budget.BudgetStatus;
import com.obraia.backend.repository.BlueprintMeasurementRepository;
import com.obraia.backend.repository.BlueprintRepository;
import com.obraia.backend.repository.BudgetRepository;
import com.obraia.backend.repository.ExpenseRepository;
import com.obraia.backend.repository.ProjectMemberRepository;
import com.obraia.backend.repository.RiskAssessmentRepository;

/**
 * The dashboard of a project. One call gathers what the plan, the budgets, the
 * expenses, the risk and the measurements say, so the screen does not have
 * to ask each part of the system on its own.
 */
@Service
public class ProjectOverviewService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ProjectAccessService accessService;
    private final WorkPlanViews views;
    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final RiskAssessmentRepository riskRepository;
    private final BlueprintRepository blueprintRepository;
    private final BlueprintMeasurementRepository measurementRepository;
    private final ProjectMemberRepository memberRepository;

    public ProjectOverviewService(ProjectAccessService accessService, WorkPlanViews views,
            BudgetRepository budgetRepository, ExpenseRepository expenseRepository,
            RiskAssessmentRepository riskRepository, BlueprintRepository blueprintRepository,
            BlueprintMeasurementRepository measurementRepository, ProjectMemberRepository memberRepository) {
        this.accessService = accessService;
        this.views = views;
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.riskRepository = riskRepository;
        this.blueprintRepository = blueprintRepository;
        this.measurementRepository = measurementRepository;
        this.memberRepository = memberRepository;
    }

    /** The dashboard of one project. */
    @Transactional(readOnly = true)
    public ProjectOverviewResponse overview(UUID projectId, UUID userId) {
        accessService.requireMembership(projectId, userId);
        return new ProjectOverviewResponse(projectId, WorkOverviews.from(views.plan(projectId)), money(projectId),
                risk(projectId), counts(projectId));
    }

    private MoneyOverview money(UUID projectId) {
        List<Budget> budgets = budgetRepository.findByProjectIdOrderByVersionDesc(projectId);
        Budget approved = budgets.stream()
                .filter(budget -> budget.getStatus() == BudgetStatus.APPROVED)
                .findFirst()
                .orElse(null);
        List<ExpenseSummaryResponse.CategoryTotal> byCategory = expenseRepository.totalsByCategory(projectId)
                .stream()
                .sorted((a, b) -> b.amount().compareTo(a.amount()))
                .map(total -> new ExpenseSummaryResponse.CategoryTotal(
                        new StatusResponse(total.category().getCode(), total.category().getLabel()),
                        total.amount(), total.count()))
                .toList();
        long expenseCount = byCategory.stream().mapToLong(ExpenseSummaryResponse.CategoryTotal::count).sum();
        BigDecimal spent = expenseRepository.sumByProjectId(projectId);
        if (approved == null) {
            return new MoneyOverview(null, budgets.size(), null, spent, null, null, expenseCount, byCategory);
        }
        BigDecimal planned = approved.getProjectedAmount();
        return new MoneyOverview(approved.getVersion(), budgets.size(), planned, spent, planned.subtract(spent),
                spentPct(spent, planned), expenseCount, byCategory);
    }

    private RiskBadge risk(UUID projectId) {
        return riskRepository.findFirstByProjectIdOrderByAssessedAtDesc(projectId).map(RiskBadge::from).orElse(null);
    }

    private ActivityCounts counts(UUID projectId) {
        List<BlueprintMeasurement> measurements = measurementRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
        long manual = measurements.stream().filter(m -> m.getSource() == MeasurementSource.MANUAL).count();
        return new ActivityCounts(blueprintRepository.countByProjectId(projectId), manual,
                measurements.size() - manual, memberRepository.findByProjectIdOrderByJoinedAtAsc(projectId).size());
    }

    /** Share of the budget already spent, with 2 decimals; null without a budget to compare with. */
    private static BigDecimal spentPct(BigDecimal spent, BigDecimal planned) {
        if (planned == null || planned.signum() == 0) {
            return null;
        }
        return spent.multiply(HUNDRED).divide(planned, 2, RoundingMode.HALF_UP);
    }
}
