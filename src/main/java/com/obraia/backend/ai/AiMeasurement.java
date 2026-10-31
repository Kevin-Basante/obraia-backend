package com.obraia.backend.ai;

import java.math.BigDecimal;

import com.obraia.backend.entity.blueprint.Blueprint;
import com.obraia.backend.entity.blueprint.BlueprintMeasurement;
import com.obraia.backend.entity.blueprint.MeasurementSource;
import com.obraia.backend.entity.catalog.ElementType;
import com.obraia.backend.entity.project.Project;

/** One element read by the AI, already in the terms of the backend. */
public record AiMeasurement(ElementType elementType, int floorNumber, BigDecimal lengthM, BigDecimal widthM,
        BigDecimal heightM, int quantity, String notes) {

    /** A measurement of the project with source "ocr". It is not saved here. */
    public BlueprintMeasurement toEntity(Project project, Blueprint blueprint) {
        BlueprintMeasurement measurement = new BlueprintMeasurement(project, MeasurementSource.OCR);
        measurement.update(blueprint, elementType, floorNumber, lengthM, widthM, heightM, quantity, notes);
        return measurement;
    }
}
