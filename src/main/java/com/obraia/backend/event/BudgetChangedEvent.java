package com.obraia.backend.event;

import java.util.UUID;

/** Published when a project gets a new budget version or one is approved. */
public record BudgetChangedEvent(UUID projectId) {
}
