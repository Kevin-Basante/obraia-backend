package com.obraia.backend.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.expense.ExpenseRequest;
import com.obraia.backend.dto.expense.ExpenseResponse;
import com.obraia.backend.entity.expense.Expense;
import com.obraia.backend.entity.expense.ExpenseCategory;
import com.obraia.backend.entity.phase.Phase;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.exception.FieldValidationException;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.ExpenseRepository;
import com.obraia.backend.repository.PhaseRepository;
import com.obraia.backend.repository.UserRepository;

/** The money really spent on a project. Every member can see and register expenses. */
@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final PhaseRepository phaseRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService accessService;
    private final Clock clock;

    public ExpenseService(ExpenseRepository expenseRepository, PhaseRepository phaseRepository,
            UserRepository userRepository, ProjectAccessService accessService, Clock clock) {
        this.expenseRepository = expenseRepository;
        this.phaseRepository = phaseRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.clock = clock;
    }

    /** Newest first. */
    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(UUID projectId, UUID userId) {
        accessService.requireMembership(projectId, userId);
        return expenseRepository.findByProjectIdOrderByExpenseDateDescCreatedAtDesc(projectId).stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    @Transactional
    public ExpenseResponse create(UUID projectId, UUID userId, ExpenseRequest request) {
        Project project = accessService.requireProject(projectId, userId);
        ProjectService.requireEditable(project);
        ExpenseCategory category = ExpenseCategory.fromCode(request.category());
        LocalDate today = LocalDate.now(clock);
        LocalDate expenseDate = request.expenseDate() == null ? today : request.expenseDate();

        Map<String, String> errors = new LinkedHashMap<>();
        if (expenseDate.isAfter(today)) {
            errors.put("expenseDate", "cannot be in the future");
        }
        Phase phase = null;
        if (request.phaseId() != null) {
            phase = phaseRepository.findByIdAndProjectId(request.phaseId(), projectId).orElse(null);
            if (phase == null) {
                errors.put("phaseId", "is not a phase of this project");
            }
        }
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }

        Expense expense = expenseRepository.save(new Expense(project, phase,
                userRepository.getReferenceById(userId), category, request.description().strip(), request.amount(),
                expenseDate, clock.instant()));
        return ExpenseResponse.from(expense);
    }

    /** Removes an expense registered by mistake. */
    @Transactional
    public void delete(UUID projectId, UUID userId, UUID expenseId) {
        Project project = accessService.requireProject(projectId, userId);
        ProjectService.requireEditable(project);
        Expense expense = expenseRepository.findByIdAndProjectId(expenseId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("EXPENSE_NOT_FOUND",
                        "The expense " + expenseId + " does not exist in this project"));
        expenseRepository.delete(expense);
    }
}
