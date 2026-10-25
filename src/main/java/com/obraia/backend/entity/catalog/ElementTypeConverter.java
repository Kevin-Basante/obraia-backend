package com.obraia.backend.entity.catalog;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the element type as its code ("wall"). */
@Converter(autoApply = true)
public class ElementTypeConverter implements AttributeConverter<ElementType, String> {

    @Override
    public String convertToDatabaseColumn(ElementType element) {
        return element == null ? null : element.getCode();
    }

    @Override
    public ElementType convertToEntityAttribute(String code) {
        return code == null ? null : ElementType.fromCode(code);
    }
}
