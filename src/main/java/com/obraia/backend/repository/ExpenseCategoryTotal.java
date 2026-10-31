package com.obraia.backend.repository;

import java.math.BigDecimal;

import com.obraia.backend.entity.expense.ExpenseCategory;

/** What was spent in one category and in how many expenses. */
public record ExpenseCategoryTotal(ExpenseCategory category, BigDecimal amount, Long count) {
}
