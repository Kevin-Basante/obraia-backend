package com.obraia.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.expense.ExpenseSummaryResponse;
import com.obraia.backend.dto.overview.ActivityCounts;
import com.obraia.backend.dto.overview.MoneyOverview;
import com.obraia.backend.dto.overview.PortfolioResponse;
import com.obraia.backend.dto.overview.ProjectCard;
import com.obraia.backend.dto.overview.ProjectOverviewResponse;
import com.obraia.backend.dto.overview.RiskBadge;
import com.obraia.backend.dto.overview.WorkOverviews;
import com.obraia.backend.dto.project.ProjectSummaryResponse;
import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.entity.blueprint.BlueprintMeasurement;
import com.obraia.backend.entity.blueprint.MeasurementSource;
import com.obraia.backend.entity.budget.Budget;
import com.obraia.backend.entity.budget.BudgetStatus;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.project.ProjectStatus;
import com.obraia.backend.entity.risk.RiskLevel;
import com.obraia.backend.repository.BlueprintMeasurementRepository;
import com.obraia.backend.repository.BlueprintRepository;
import com.obraia.backend.repository.BudgetRepository;
import com.obraia.backend.repository.ExpenseRepository;
import com.obraia.backend.repository.ProjectMemberRepository;
import com.obraia.backend.repository.ProjectRepository;
import com.obraia.backend.repository.RiskAssessmentRepository;

/**
 * The dashboards. One call gathers what the plan, the budgets, the
 * expenses, the risk and the measurements say, so the screen does not have
 * to ask each part of the system on its own.
 */
@Service
public class ProjectOverviewService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ProjectAccessService accessService;
    private final ProjectRepository projectRepository;
    private final WorkPlanViews views;
    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final RiskAssessmentRepository riskRepository;
    private final BlueprintRepository blueprintRepository;
    private final BlueprintMeasurementRepository measurementRepository;
    private final ProjectMemberRepository memberRepository;

    public ProjectOverviewService(ProjectAccessService accessService, ProjectRepository projectRepository,
            WorkPlanViews views, BudgetRepository budgetRepository, ExpenseRepository expenseRepository,
            RiskAssessmentRepository riskRepository, BlueprintRepository blueprintRepository,
            BlueprintMeasurementRepository measurementRepository, ProjectMemberRepository memberRepository) {
        this.accessService = accessService;
        this.projectRepository = projectRepository;
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
        return overviewOf(projectId);
    }

    /**
     * The same dashboard without checking who asks. Only for callers that
     * already checked the access, like the reports opened with a shared link.
     */
    @Transactional(readOnly = true)
    public ProjectOverviewResponse overviewOf(UUID projectId) {
        return new ProjectOverviewResponse(projectId, WorkOverviews.from(views.plan(projectId)), money(projectId),
                risk(projectId), counts(projectId));
    }

    /** Every project of the user, newest first, with the totals of all of them. */
    @Transactional(readOnly = true)
    public PortfolioResponse portfolio(UUID userId) {
        List<ProjectCard> cards = projectRepository.findVisibleTo(userId).stream().map(this::card).toList();
        int active = (int) cards.stream()
                .filter(card -> ProjectStatus.IN_PROGRESS.getCode().equals(card.project().status().code()))
                .count();
        int atRisk = (int) cards.stream().filter(card -> isHighRisk(card.risk())).count();
        BigDecimal approved = cards.stream()
                .map(ProjectCard::approvedBudget)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal spent = cards.stream().map(ProjectCard::totalSpent).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PortfolioResponse(cards.size(), active, atRisk, approved, spent, cards);
    }

    private ProjectCard card(Project project) {
        UUID projectId = project.getId();
        BigDecimal approved = approvedBudget(projectId).map(Budget::getProjectedAmount).orElse(null);
        BigDecimal spent = expenseRepository.sumByProjectId(projectId);
        return new ProjectCard(ProjectSummaryResponse.from(project), views.plan(projectId).progressPct(), approved,
                spent, spentPct(spent, approved), risk(projectId));
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

    private Optional<Budget> approvedBudget(UUID projectId) {
        return budgetRepository.findByProjectIdAndStatus(projectId, BudgetStatus.APPROVED);
    }

    /** Share of the budget already spent, with 2 decimals; null without a budget to compare with. */
    private static BigDecimal spentPct(BigDecimal spent, BigDecimal planned) {
        if (planned == null || planned.signum() == 0) {
            return null;
        }
        return spent.multiply(HUNDRED).divide(planned, 2, RoundingMode.HALF_UP);
    }

    private static boolean isHighRisk(RiskBadge risk) {
        return risk != null && (RiskLevel.HIGH.getCode().equals(risk.level().code())
                || RiskLevel.CRITICAL.getCode().equals(risk.level().code()));
    }
}
