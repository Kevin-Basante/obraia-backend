package com.obraia.backend.entity.catalog;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the size as "small" or "large". */
@Converter(autoApply = true)
public class SizeCategoryConverter implements AttributeConverter<SizeCategory, String> {

    @Override
    public String convertToDatabaseColumn(SizeCategory size) {
        return size == null ? null : size.getCode();
    }

    @Override
    public SizeCategory convertToEntityAttribute(String code) {
        return code == null ? null : SizeCategory.fromCode(code);
    }
}
