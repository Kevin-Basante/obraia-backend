package com.obraia.backend.dto.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A new expense.
 *
 * @param category    materials, labor, equipment, tax or other
 * @param amount      in COP, more than 0
 * @param expenseDate today when it is missing; it cannot be in the future
 * @param phaseId     optional phase of the project the expense belongs to
 */
public record ExpenseRequest(
        @NotBlank String category,
        @NotBlank @Size(max = 255) String description,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @DecimalMax("999999999999.99")
        @Digits(integer = 12, fraction = 2) BigDecimal amount,
        LocalDate expenseDate,
        UUID phaseId) {
}
