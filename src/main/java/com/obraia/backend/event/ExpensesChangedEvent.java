package com.obraia.backend.event;

import java.util.UUID;

/** Published when an expense of the project is registered or deleted. */
public record ExpensesChangedEvent(UUID projectId) {
}
