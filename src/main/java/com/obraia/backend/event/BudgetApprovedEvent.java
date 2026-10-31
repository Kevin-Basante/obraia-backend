package com.obraia.backend.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Published when a budget version is approved. */
public record BudgetApprovedEvent(UUID projectId, int version, BigDecimal projectedAmount) {
}
