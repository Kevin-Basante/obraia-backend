package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;

import com.obraia.backend.dto.budget.BudgetResponse;
import com.obraia.backend.entity.budget.Budget;
import com.obraia.backend.entity.budget.BudgetItem;
import com.obraia.backend.entity.budget.BudgetItemType;
import com.obraia.backend.entity.budget.BudgetStatus;
import com.obraia.backend.entity.budget.ProjectMaterial;
import com.obraia.backend.entity.catalog.MaterialCategory;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.project.ProjectStatus;
import com.obraia.backend.event.BudgetChangedEvent;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.quotation.MaterialNeed;
import com.obraia.backend.quotation.MaterialTakeoff;
import com.obraia.backend.quotation.MaterialsCost;
import com.obraia.backend.quotation.Quotation;
import com.obraia.backend.quotation.QuotationFacade;
import com.obraia.backend.quotation.WasteCost;
import com.obraia.backend.repository.BudgetItemRepository;
import com.obraia.backend.repository.BudgetRepository;
import com.obraia.backend.repository.ProjectMaterialRepository;
import com.obraia.backend.support.BudgetFixtures;
import com.obraia.backend.support.CatalogFixtures;
import com.obraia.backend.support.ProjectFixtures;

class BudgetServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"),
            ZoneId.of("America/Bogota"));

    private final UUID userId = UUID.randomUUID();
    private final Project project = ProjectFixtures.project(userId, "Casa Presupuesto");
    private final List<MaterialNeed> needs = List.of(new MaterialNeed(
            CatalogFixtures.material("Gravilla", MaterialCategory.AGGREGATES, "115000"), new BigDecimal("2.00"),
            new BigDecimal("0.10")));

    private BudgetRepository budgets;
    private BudgetItemRepository items;
    private ProjectMaterialRepository materials;
    private QuotationFacade facade;
    private BudgetVersions versions;
    private ProjectAccessService access;
    private ApplicationEventPublisher events;
    private BudgetService service;

    @BeforeEach
    void setUp() {
        budgets = mock(BudgetRepository.class);
        items = mock(BudgetItemRepository.class);
        materials = mock(ProjectMaterialRepository.class);
        facade = mock(QuotationFacade.class);
        versions = mock(BudgetVersions.class);
        access = mock(ProjectAccessService.class);
        events = mock(ApplicationEventPublisher.class);
        when(access.requireProject(project.getId(), userId)).thenReturn(project);
        when(budgets.save(any())).thenAnswer(call -> call.getArgument(0));
        when(budgets.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        service = new BudgetService(budgets, items, materials, facade, versions, access, events, CLOCK);
    }

    @Test
    void createSavesTheNextVersionWithOneRowPerLayer() {
        when(facade.quoteProject(project)).thenReturn(quotation());
        when(budgets.findLastVersion(project.getId())).thenReturn(2);

        BudgetResponse response = service.create(project.getId(), userId);

        ArgumentCaptor<Budget> saved = ArgumentCaptor.forClass(Budget.class);
        verify(budgets).save(saved.capture());
        assertEquals(3, saved.getValue().getVersion());
        assertEquals(BudgetStatus.DRAFT, saved.getValue().getStatus());
        // 230,000 of gravel plus 11,500 of waste.
        assertEquals(new BigDecimal("241500.00"), saved.getValue().getProjectedAmount());
        assertEquals(CLOCK.instant(), saved.getValue().getCreatedAt());
        ArgumentCaptor<BudgetItem> rows = ArgumentCaptor.forClass(BudgetItem.class);
        verify(items, times(2)).save(rows.capture());
        assertEquals(List.of(BudgetItemType.MATERIALS, BudgetItemType.WASTE),
                rows.getAllValues().stream().map(BudgetItem::getItemType).toList());
        assertEquals(2, rows.getAllValues().get(1).getSequenceOrder());
        assertNotNull(response);
    }

    @Test
    void createReplacesTheMaterialsWithWasteIncluded() {
        when(facade.quoteProject(project)).thenReturn(quotation());

        service.create(project.getId(), userId);

        InOrder order = inOrder(materials);
        order.verify(materials).deleteByProjectId(project.getId());
        ArgumentCaptor<ProjectMaterial> row = ArgumentCaptor.forClass(ProjectMaterial.class);
        order.verify(materials).save(row.capture());
        assertEquals(new BigDecimal("2.10"), row.getValue().getCalculatedQuantity());
        assertEquals(new BigDecimal("115000"), row.getValue().getUnitPriceSnapshot());
    }

    @Test
    void createAnnouncesTheChangeSoTheCacheIsCleared() {
        when(facade.quoteProject(project)).thenReturn(quotation());

        service.create(project.getId(), userId);

        verify(events).publishEvent(new BudgetChangedEvent(project.getId()));
    }

    @Test
    void closedProjectIsNotQuoted() {
        Project closed = ProjectFixtures.project(userId, "Casa Cerrada", ProjectStatus.COMPLETED);
        when(access.requireProject(closed.getId(), userId)).thenReturn(closed);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.create(closed.getId(), userId));

        assertEquals("PROJECT_CLOSED", ex.getCode());
        verifyNoInteractions(facade);
    }

    @Test
    void previewDoesNotSaveAnything() {
        when(facade.quoteProject(project)).thenReturn(quotation());

        assertEquals(new BigDecimal("241500.00"), service.preview(project.getId(), userId).projectedAmount());
        verify(budgets, never()).save(any());
        verifyNoInteractions(materials, events);
    }

    @Test
    void approvingSupersedesTheVersionApprovedBefore() {
        Budget previous = BudgetFixtures.approved(project, 1, "1000000");
        Budget draft = BudgetFixtures.budget(project, 2, "1200000");
        when(budgets.findByIdAndProjectId(draft.getId(), project.getId())).thenReturn(Optional.of(draft));
        when(budgets.findByProjectIdAndStatus(project.getId(), BudgetStatus.APPROVED))
                .thenReturn(Optional.of(previous));

        BudgetResponse response = service.approve(project.getId(), userId, draft.getId());

        assertEquals(BudgetStatus.SUPERSEDED, previous.getStatus());
        assertEquals("approved", response.status().code());
        assertEquals(CLOCK.instant(), draft.getApprovedAt());
        // The old one is saved first: only one budget can be approved.
        InOrder order = inOrder(budgets);
        order.verify(budgets).saveAndFlush(previous);
        order.verify(budgets).saveAndFlush(draft);
        verify(events).publishEvent(new BudgetChangedEvent(project.getId()));
    }

    @Test
    void firstApprovalHasNothingToSupersede() {
        Budget draft = BudgetFixtures.budget(project, 1, "1200000");
        when(budgets.findByIdAndProjectId(draft.getId(), project.getId())).thenReturn(Optional.of(draft));
        when(budgets.findByProjectIdAndStatus(project.getId(), BudgetStatus.APPROVED)).thenReturn(Optional.empty());

        service.approve(project.getId(), userId, draft.getId());

        assertEquals(BudgetStatus.APPROVED, draft.getStatus());
        verify(budgets, times(1)).saveAndFlush(any());
    }

    @Test
    void onlyADraftCanBeApproved() {
        Budget approved = BudgetFixtures.approved(project, 1, "1000000");
        when(budgets.findByIdAndProjectId(approved.getId(), project.getId())).thenReturn(Optional.of(approved));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.approve(project.getId(), userId, approved.getId()));

        assertEquals("BUDGET_NOT_DRAFT", ex.getCode());
        verify(budgets, never()).saveAndFlush(any());
    }

    @Test
    void budgetOfAnotherProjectIsNotFound() {
        UUID budgetId = UUID.randomUUID();
        when(budgets.findByIdAndProjectId(budgetId, project.getId())).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.get(project.getId(), userId, budgetId));

        assertEquals("BUDGET_NOT_FOUND", ex.getCode());
    }

    @Test
    void listComesFromTheCachedVersions() {
        when(versions.list(project.getId())).thenReturn(List.of());

        assertEquals(List.of(), service.list(project.getId(), userId));
        verify(access).requireMembership(project.getId(), userId);
        verifyNoInteractions(budgets);
    }

    private Quotation quotation() {
        return Quotation.builder().projectId(project.getId())
                .location("Cali", new BigDecimal("3.00"))
                .workingDays(26, Quotation.DaysSource.STANDARD_PLAN)
                .takeoff(new MaterialTakeoff(needs, List.of()))
                .cost(new WasteCost(new MaterialsCost(needs), needs))
                .build();
    }
}
