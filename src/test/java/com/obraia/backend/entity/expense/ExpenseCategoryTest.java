package com.obraia.backend.entity.expense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.obraia.backend.exception.BadRequestException;

class ExpenseCategoryTest {

    private final ExpenseCategoryConverter converter = new ExpenseCategoryConverter();

    @Test
    void everyCategoryGoesAndComesBackByItsCode() {
        for (ExpenseCategory category : ExpenseCategory.values()) {
            String code = converter.convertToDatabaseColumn(category);
            assertEquals(category, converter.convertToEntityAttribute(code));
        }
        assertEquals("labor", ExpenseCategory.LABOR.getCode());
        assertEquals("Taxes", ExpenseCategory.TAX.getLabel());
    }

    @Test
    void nullStaysNull() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void unknownCodeIsABadRequest() {
        BadRequestException ex = assertThrows(BadRequestException.class, () -> ExpenseCategory.fromCode("food"));
        assertEquals("INVALID_EXPENSE_CATEGORY", ex.getCode());
    }
}
