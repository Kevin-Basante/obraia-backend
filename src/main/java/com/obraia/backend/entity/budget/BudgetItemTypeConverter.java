package com.obraia.backend.entity.budget;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the type as its code ("labor"), the value allowed by the CHECK constraint. */
@Converter(autoApply = true)
public class BudgetItemTypeConverter implements AttributeConverter<BudgetItemType, String> {

    @Override
    public String convertToDatabaseColumn(BudgetItemType type) {
        return type == null ? null : type.getCode();
    }

    @Override
    public BudgetItemType convertToEntityAttribute(String code) {
        return code == null ? null : BudgetItemType.fromCode(code);
    }
}
