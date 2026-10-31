package com.obraia.backend.support;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.obraia.backend.entity.budget.Budget;
import com.obraia.backend.entity.project.Project;

/** Builds budgets for unit tests. Ids are set by reflection. */
public final class BudgetFixtures {

    private BudgetFixtures() {
    }

    public static Budget budget(Project project, int version, String amount) {
        Budget budget = new Budget(project, version, new BigDecimal(amount), Instant.parse("2026-11-02T13:00:00Z"));
        set(budget, "id", UUID.randomUUID());
        return budget;
    }

    public static Budget approved(Project project, int version, String amount) {
        Budget budget = budget(project, version, amount);
        budget.approve(Instant.parse("2026-11-03T13:00:00Z"));
        return budget;
    }

    private static void set(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
