package com.obraia.backend.entity.blueprint;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the measurement source as its code ("manual"). */
@Converter(autoApply = true)
public class MeasurementSourceConverter implements AttributeConverter<MeasurementSource, String> {

    @Override
    public String convertToDatabaseColumn(MeasurementSource source) {
        return source == null ? null : source.getCode();
    }

    @Override
    public MeasurementSource convertToEntityAttribute(String code) {
        return code == null ? null : MeasurementSource.fromCode(code);
    }
}
