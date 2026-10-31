package com.obraia.backend.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.expense.ExpenseRequest;
import com.obraia.backend.dto.expense.ExpenseResponse;
import com.obraia.backend.entity.expense.Expense;
import com.obraia.backend.entity.expense.ExpenseCategory;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.repository.ExpenseRepository;
import com.obraia.backend.repository.UserRepository;

/** The money really spent on a project. Every member can see and register expenses. */
@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService accessService;
    private final Clock clock;

    public ExpenseService(ExpenseRepository expenseRepository, UserRepository userRepository,
            ProjectAccessService accessService, Clock clock) {
        this.expenseRepository = expenseRepository;
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
        LocalDate expenseDate = request.expenseDate() == null ? LocalDate.now(clock) : request.expenseDate();
        Expense expense = expenseRepository.save(new Expense(project, null, userRepository.getReferenceById(userId),
                ExpenseCategory.fromCode(request.category()), request.description().strip(), request.amount(),
                expenseDate, clock.instant()));
        return ExpenseResponse.from(expense);
    }
}
