package com.obraia.backend.entity.phase;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the status as its code ("in_progress"), the value allowed by the CHECK constraints. */
@Converter(autoApply = true)
public class WorkStatusConverter implements AttributeConverter<WorkStatus, String> {

    @Override
    public String convertToDatabaseColumn(WorkStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public WorkStatus convertToEntityAttribute(String code) {
        return code == null ? null : WorkStatus.fromCode(code);
    }
}
