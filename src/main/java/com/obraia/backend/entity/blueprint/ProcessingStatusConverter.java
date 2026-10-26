package com.obraia.backend.entity.blueprint;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the processing status as its code ("pending"). */
@Converter(autoApply = true)
public class ProcessingStatusConverter implements AttributeConverter<ProcessingStatus, String> {

    @Override
    public String convertToDatabaseColumn(ProcessingStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public ProcessingStatus convertToEntityAttribute(String code) {
        return code == null ? null : ProcessingStatus.fromCode(code);
    }
}
