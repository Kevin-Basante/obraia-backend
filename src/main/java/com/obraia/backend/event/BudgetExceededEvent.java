package com.obraia.backend.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Published when the expenses of a project pass its approved budget for the first time. */
public record BudgetExceededEvent(UUID projectId, BigDecimal approvedBudget, BigDecimal spent) {
}
