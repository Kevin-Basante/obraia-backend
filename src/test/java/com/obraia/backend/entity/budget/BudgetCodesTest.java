package com.obraia.backend.entity.budget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.obraia.backend.exception.BadRequestException;

/** The codes must match the CHECK constraints of V2 and V5. */
class BudgetCodesTest {

    @Test
    void statusesUseTheDatabaseCodes() {
        assertEquals("superseded", BudgetStatus.SUPERSEDED.getCode());
        assertEquals(BudgetStatus.DRAFT, new BudgetStatusConverter().convertToEntityAttribute("draft"));
        assertNull(new BudgetStatusConverter().convertToDatabaseColumn(null));
    }

    @Test
    void itemTypesIncludeTheNewLayers() {
        assertEquals(BudgetItemType.WASTE, BudgetItemType.fromCode("waste"));
        assertEquals("transport", new BudgetItemTypeConverter().convertToDatabaseColumn(BudgetItemType.TRANSPORT));
    }

    @Test
    void unknownCodesAreRejected() {
        assertThrows(BadRequestException.class, () -> BudgetStatus.fromCode("closed"));
        assertThrows(BadRequestException.class, () -> BudgetItemType.fromCode("fees"));
    }
}
