package com.obraia.backend.entity.budget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.obraia.backend.support.BudgetFixtures;
import com.obraia.backend.support.ProjectFixtures;

class BudgetActualTest {

    private final Budget budget = BudgetFixtures.approved(ProjectFixtures.project(UUID.randomUUID(), "Casa"), 1,
            "1000000");

    @Test
    void startsAtZeroAndTakesTheTotalOfTheExpenses() {
        assertEquals(BigDecimal.ZERO, budget.getActualAmount());

        budget.recordActual(new BigDecimal("250000.00"));

        assertEquals(new BigDecimal("250000.00"), budget.getActualAmount());
    }

    @Test
    void negativeTotalIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> budget.recordActual(new BigDecimal("-1")));
    }
}
