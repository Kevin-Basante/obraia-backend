package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.obraia.backend.dto.expense.ExpenseRequest;
import com.obraia.backend.dto.expense.ExpenseResponse;
import com.obraia.backend.entity.expense.Expense;
import com.obraia.backend.entity.expense.ExpenseCategory;
import com.obraia.backend.entity.phase.Phase;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.project.ProjectStatus;
import com.obraia.backend.exception.BadRequestException;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.exception.FieldValidationException;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.ExpenseRepository;
import com.obraia.backend.repository.PhaseRepository;
import com.obraia.backend.repository.UserRepository;
import com.obraia.backend.support.PhaseFixtures;
import com.obraia.backend.support.ProjectFixtures;
import com.obraia.backend.support.TestUsers;

class ExpenseServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"),
            ZoneId.of("America/Bogota"));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    private final UUID userId = UUID.randomUUID();
    private final Project project = ProjectFixtures.project(userId, "Casa Gastos");
    private final Phase phase = PhaseFixtures.phase(project, 1, "Cimentación");

    private ExpenseRepository expenses;
    private PhaseRepository phases;
    private ProjectAccessService access;
    private ExpenseService service;

    @BeforeEach
    void setUp() {
        expenses = mock(ExpenseRepository.class);
        phases = mock(PhaseRepository.class);
        access = mock(ProjectAccessService.class);
        UserRepository users = mock(UserRepository.class);
        when(access.requireProject(project.getId(), userId)).thenReturn(project);
        when(users.getReferenceById(userId)).thenReturn(TestUsers.verifiedUser("hash"));
        when(expenses.save(any())).thenAnswer(call -> call.getArgument(0));
        when(phases.findByIdAndProjectId(phase.getId(), project.getId())).thenReturn(Optional.of(phase));
        service = new ExpenseService(expenses, phases, users, access, CLOCK);
    }

    private static ExpenseRequest request(String category, String amount, LocalDate date) {
        return new ExpenseRequest(category, "  Cemento gris x 20  ", new BigDecimal(amount), date, null);
    }

    @Test
    void createSavesTheExpenseOfToday() {
        ExpenseResponse response = service.create(project.getId(), userId, request("materials", "650000", null));

        assertEquals("materials", response.category().code());
        assertEquals("Materials", response.category().label());
        assertEquals("Cemento gris x 20", response.description());
        assertEquals(new BigDecimal("650000"), response.amount());
        assertEquals(TODAY, response.expenseDate());
        assertEquals(CLOCK.instant(), response.createdAt());
    }

    @Test
    void createKeepsTheDateGiven() {
        LocalDate date = LocalDate.of(2026, 11, 3);

        assertEquals(date, service.create(project.getId(), userId, request("labor", "1", date)).expenseDate());
    }

    @Test
    void expenseCanBelongToAPhaseOfTheProject() {
        ExpenseRequest request = new ExpenseRequest("labor", "Excavación", new BigDecimal("400000"), null,
                phase.getId());

        ExpenseResponse response = service.create(project.getId(), userId, request);

        assertEquals(phase.getId(), response.phaseId());
        assertEquals("Cimentación", response.phaseName());
    }

    @Test
    void phaseOfAnotherProjectAndFutureDateAreRejected() {
        ExpenseRequest request = new ExpenseRequest("labor", "Excavación", new BigDecimal("400000"),
                TODAY.plusDays(1), UUID.randomUUID());

        FieldValidationException ex = assertThrows(FieldValidationException.class,
                () -> service.create(project.getId(), userId, request));

        assertEquals("cannot be in the future", ex.getFields().get("expenseDate"));
        assertEquals("is not a phase of this project", ex.getFields().get("phaseId"));
        verify(expenses, never()).save(any());
    }

    @Test
    void unknownCategoryIsABadRequest() {
        assertThrows(BadRequestException.class,
                () -> service.create(project.getId(), userId, request("food", "1000", null)));
        verify(expenses, never()).save(any());
    }

    @Test
    void closedProjectCannotGetExpenses() {
        Project closed = ProjectFixtures.project(userId, "Casa Cerrada", ProjectStatus.COMPLETED);
        when(access.requireProject(closed.getId(), userId)).thenReturn(closed);

        assertThrows(ConflictException.class,
                () -> service.create(closed.getId(), userId, request("materials", "1000", null)));
    }

    @Test
    void deleteRemovesTheExpenseOfTheProject() {
        Expense expense = new Expense(project, null, null, ExpenseCategory.OTHER, "Error", BigDecimal.TEN, TODAY,
                CLOCK.instant());
        UUID expenseId = UUID.randomUUID();
        when(expenses.findByIdAndProjectId(expenseId, project.getId())).thenReturn(Optional.of(expense));

        service.delete(project.getId(), userId, expenseId);

        verify(expenses).delete(expense);
    }

    @Test
    void deletingAnUnknownExpenseIsNotFound() {
        UUID expenseId = UUID.randomUUID();
        when(expenses.findByIdAndProjectId(expenseId, project.getId())).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.delete(project.getId(), userId, expenseId));

        assertEquals("EXPENSE_NOT_FOUND", ex.getCode());
    }

    @Test
    void listChecksTheMembershipAndKeepsTheOrder() {
        Expense older = new Expense(project, null, null, ExpenseCategory.TAX, "IVA", new BigDecimal("10"),
                TODAY.minusDays(3), CLOCK.instant());
        Expense newer = new Expense(project, null, null, ExpenseCategory.OTHER, "Agua", new BigDecimal("20"),
                TODAY, CLOCK.instant());
        when(expenses.findByProjectIdOrderByExpenseDateDescCreatedAtDesc(project.getId()))
                .thenReturn(List.of(newer, older));

        List<ExpenseResponse> list = service.list(project.getId(), userId);

        verify(access).requireMembership(project.getId(), userId);
        assertEquals(List.of("Agua", "IVA"), list.stream().map(ExpenseResponse::description).toList());
        assertEquals(null, list.get(0).registeredBy());
    }
}
