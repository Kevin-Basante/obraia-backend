package com.obraia.backend.entity.expense;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the category as its code ("materials"), the value allowed by the CHECK constraint. */
@Converter(autoApply = true)
public class ExpenseCategoryConverter implements AttributeConverter<ExpenseCategory, String> {

    @Override
    public String convertToDatabaseColumn(ExpenseCategory category) {
        return category == null ? null : category.getCode();
    }

    @Override
    public ExpenseCategory convertToEntityAttribute(String code) {
        return code == null ? null : ExpenseCategory.fromCode(code);
    }
}
