package com.obraia.backend.dto.expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.entity.User;
import com.obraia.backend.entity.expense.Expense;

/** An expense, with the name of its phase and of who registered it. */
public record ExpenseResponse(UUID id, StatusResponse category, String description, BigDecimal amount,
        LocalDate expenseDate, UUID phaseId, String phaseName, String registeredBy, Instant createdAt) {

    public static ExpenseResponse from(Expense expense) {
        User user = expense.getRegisteredBy();
        return new ExpenseResponse(expense.getId(),
                new StatusResponse(expense.getCategory().getCode(), expense.getCategory().getLabel()),
                expense.getDescription(), expense.getAmount(), expense.getExpenseDate(),
                expense.getPhase() == null ? null : expense.getPhase().getId(),
                expense.getPhase() == null ? null : expense.getPhase().getName(),
                user == null ? null : user.getFirstName() + " " + user.getLastName(),
                expense.getCreatedAt());
    }
}
