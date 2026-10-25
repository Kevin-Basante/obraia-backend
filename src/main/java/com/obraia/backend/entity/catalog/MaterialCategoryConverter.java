package com.obraia.backend.entity.catalog;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the category as its code ("steel"), the value allowed by the CHECK constraint. */
@Converter(autoApply = true)
public class MaterialCategoryConverter implements AttributeConverter<MaterialCategory, String> {

    @Override
    public String convertToDatabaseColumn(MaterialCategory category) {
        return category == null ? null : category.getCode();
    }

    @Override
    public MaterialCategory convertToEntityAttribute(String code) {
        return code == null ? null : MaterialCategory.fromCode(code);
    }
}
