package com.obraia.backend.entity.project;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the status as its code ("in_progress"), the value allowed by the CHECK constraint. */
@Converter(autoApply = true)
public class ProjectStatusConverter implements AttributeConverter<ProjectStatus, String> {

    @Override
    public String convertToDatabaseColumn(ProjectStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public ProjectStatus convertToEntityAttribute(String code) {
        return code == null ? null : ProjectStatus.fromCode(code);
    }
}
