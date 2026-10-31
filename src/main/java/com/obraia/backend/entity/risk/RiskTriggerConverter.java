package com.obraia.backend.entity.risk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the trigger as its code ("expense"), the value allowed by the CHECK constraint. */
@Converter(autoApply = true)
public class RiskTriggerConverter implements AttributeConverter<RiskTrigger, String> {

    @Override
    public String convertToDatabaseColumn(RiskTrigger trigger) {
        return trigger == null ? null : trigger.getCode();
    }

    @Override
    public RiskTrigger convertToEntityAttribute(String code) {
        return code == null ? null : RiskTrigger.fromCode(code);
    }
}
