package com.obraia.backend.entity.blueprint;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the file type as its code ("vector_pdf"). */
@Converter(autoApply = true)
public class BlueprintFileTypeConverter implements AttributeConverter<BlueprintFileType, String> {

    @Override
    public String convertToDatabaseColumn(BlueprintFileType type) {
        return type == null ? null : type.getCode();
    }

    @Override
    public BlueprintFileType convertToEntityAttribute(String code) {
        return code == null ? null : BlueprintFileType.fromCode(code);
    }
}
