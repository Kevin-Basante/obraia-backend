package com.obraia.backend.dto.catalog;

import java.util.List;

import com.obraia.backend.entity.blueprint.MeasurementRules;
import com.obraia.backend.entity.catalog.ElementType;

/** A building element, its base unit and the dimensions a measurement of it needs. */
public record ElementTypeResponse(String code, String label, String baseUnit, List<String> requiredDimensions) {

    public static ElementTypeResponse from(ElementType element) {
        return new ElementTypeResponse(element.getCode(), element.getLabel(), MeasurementRules.baseUnit(element),
                MeasurementRules.requiredDimensions(element));
    }
}
