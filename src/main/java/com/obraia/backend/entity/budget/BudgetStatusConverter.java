package com.obraia.backend.entity.budget;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the status as its code ("draft"), the value allowed by the CHECK constraint. */
@Converter(autoApply = true)
public class BudgetStatusConverter implements AttributeConverter<BudgetStatus, String> {

    @Override
    public String convertToDatabaseColumn(BudgetStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public BudgetStatus convertToEntityAttribute(String code) {
        return code == null ? null : BudgetStatus.fromCode(code);
    }
}
