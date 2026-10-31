package com.obraia.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.obraia.backend.dto.phase.PhaseResponse;
import com.obraia.backend.dto.phase.TaskResponse;
import com.obraia.backend.dto.phase.WorkPlanResponse;
import com.obraia.backend.entity.budget.Budget;
import com.obraia.backend.entity.budget.BudgetStatus;
import com.obraia.backend.repository.BudgetRepository;
import com.obraia.backend.repository.ExpenseRepository;
import com.obraia.backend.risk.ProjectSnapshot;

/**
 * Reads what the risk strategies need: the plan with its progress, the
 * late work, the approved budget and the expenses.
 */
@Component
class ProjectSnapshots {

    private final WorkPlanViews views;
    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;

    ProjectSnapshots(WorkPlanViews views, BudgetRepository budgetRepository, ExpenseRepository expenseRepository) {
        this.views = views;
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
    }

    ProjectSnapshot read(UUID projectId) {
        LocalDate today = views.today();
        WorkPlanResponse plan = views.plan(projectId);
        BigDecimal approved = budgetRepository.findByProjectIdAndStatus(projectId, BudgetStatus.APPROVED)
                .map(Budget::getProjectedAmount)
                .orElse(null);
        return new ProjectSnapshot(today, plan.plannedStartDate(), plan.plannedEndDate(), plan.progressPct(),
                lateWork(plan, today), approved, expenseRepository.sumByProjectId(projectId));
    }

    /** Late tasks, and late phases that have no tasks to show it. */
    private static List<ProjectSnapshot.LateWork> lateWork(WorkPlanResponse plan, LocalDate today) {
        List<ProjectSnapshot.LateWork> late = new ArrayList<>();
        for (PhaseResponse phase : plan.phases()) {
            if (phase.tasks().isEmpty() && phase.late()) {
                late.add(new ProjectSnapshot.LateWork(phase.name(), daysLate(phase.plannedEndDate(), today)));
            }
            for (TaskResponse task : phase.tasks()) {
                if (task.late()) {
                    late.add(new ProjectSnapshot.LateWork(task.name(), daysLate(task.plannedEndDate(), today)));
                }
            }
        }
        return late;
    }

    private static int daysLate(LocalDate plannedEnd, LocalDate today) {
        return (int) ChronoUnit.DAYS.between(plannedEnd, today);
    }
}
