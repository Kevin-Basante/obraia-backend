package com.obraia.backend.entity.risk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the level as its code ("high"), the value allowed by the CHECK constraint. */
@Converter(autoApply = true)
public class RiskLevelConverter implements AttributeConverter<RiskLevel, String> {

    @Override
    public String convertToDatabaseColumn(RiskLevel level) {
        return level == null ? null : level.getCode();
    }

    @Override
    public RiskLevel convertToEntityAttribute(String code) {
        return code == null ? null : RiskLevel.fromCode(code);
    }
}
