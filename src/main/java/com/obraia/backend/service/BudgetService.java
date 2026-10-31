package com.obraia.backend.service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.budget.BudgetResponse;
import com.obraia.backend.dto.budget.BudgetSummaryResponse;
import com.obraia.backend.dto.budget.QuotationResponse;
import com.obraia.backend.entity.budget.Budget;
import com.obraia.backend.entity.budget.BudgetItem;
import com.obraia.backend.entity.budget.ProjectMaterial;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.event.BudgetChangedEvent;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.quotation.CostLine;
import com.obraia.backend.quotation.MaterialNeed;
import com.obraia.backend.quotation.Quotation;
import com.obraia.backend.quotation.QuotationFacade;
import com.obraia.backend.repository.BudgetItemRepository;
import com.obraia.backend.repository.BudgetRepository;
import com.obraia.backend.repository.ProjectMaterialRepository;

/**
 * The budget versions of a project. The cost comes from the quotation
 * facade; this service saves it as a new version and handles approval.
 */
@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetItemRepository itemRepository;
    private final ProjectMaterialRepository materialRepository;
    private final QuotationFacade quotationFacade;
    private final BudgetVersions versions;
    private final ProjectAccessService accessService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public BudgetService(BudgetRepository budgetRepository, BudgetItemRepository itemRepository,
            ProjectMaterialRepository materialRepository, QuotationFacade quotationFacade, BudgetVersions versions,
            ProjectAccessService accessService, ApplicationEventPublisher events, Clock clock) {
        this.budgetRepository = budgetRepository;
        this.itemRepository = itemRepository;
        this.materialRepository = materialRepository;
        this.quotationFacade = quotationFacade;
        this.versions = versions;
        this.accessService = accessService;
        this.events = events;
        this.clock = clock;
    }

    /** Newest version first. */
    @Transactional(readOnly = true)
    public List<BudgetSummaryResponse> list(UUID projectId, UUID userId) {
        accessService.requireMembership(projectId, userId);
        return versions.list(projectId);
    }

    @Transactional(readOnly = true)
    public BudgetResponse get(UUID projectId, UUID userId, UUID budgetId) {
        accessService.requireMembership(projectId, userId);
        Budget budget = requireBudget(projectId, budgetId);
        return BudgetResponse.from(budget, itemRepository.findByBudgetIdOrderBySequenceOrderAsc(budgetId));
    }

    /** What {@link #create} would save, without saving anything. */
    @Transactional(readOnly = true)
    public QuotationResponse preview(UUID projectId, UUID userId) {
        Project project = accessService.requireProject(projectId, userId);
        return QuotationResponse.from(quotationFacade.quoteProject(project));
    }

    /** Quotes the project and saves the result as a new draft version. */
    @Transactional
    public BudgetResponse create(UUID projectId, UUID userId) {
        Project project = accessService.requireProject(projectId, userId);
        ProjectService.requireEditable(project);
        Quotation quotation = quotationFacade.quoteProject(project);
        Budget budget = budgetRepository.save(new Budget(project, budgetRepository.findLastVersion(projectId) + 1,
                quotation.total(), clock.instant()));
        int order = 1;
        for (CostLine line : quotation.lines()) {
            itemRepository.save(new BudgetItem(budget, line.type(), line.description(), line.amount(),
                    line.percentage(), order++));
        }
        replaceMaterials(project, quotation.getMaterials());
        budgetRepository.flush();
        events.publishEvent(new BudgetChangedEvent(projectId));
        return BudgetResponse.from(budget, itemRepository.findByBudgetIdOrderBySequenceOrderAsc(budget.getId()));
    }

    private void replaceMaterials(Project project, List<MaterialNeed> needs) {
        materialRepository.deleteByProjectId(project.getId());
        for (MaterialNeed need : needs) {
            materialRepository.save(new ProjectMaterial(project, need.material(), need.totalQuantity(),
                    need.material().getReferencePrice()));
        }
    }

    private Budget requireBudget(UUID projectId, UUID budgetId) {
        return budgetRepository.findByIdAndProjectId(budgetId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("BUDGET_NOT_FOUND",
                        "The budget " + budgetId + " does not exist in this project"));
    }
}
