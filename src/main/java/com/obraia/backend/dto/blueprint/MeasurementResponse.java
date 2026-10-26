package com.obraia.backend.dto.blueprint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.obraia.backend.entity.blueprint.Blueprint;
import com.obraia.backend.entity.blueprint.BlueprintMeasurement;
import com.obraia.backend.entity.blueprint.MeasurementRules;

/** A measurement with its amount already calculated in the base unit. */
public record MeasurementResponse(UUID id, UUID blueprintId, String blueprintFilename, String elementType,
        String elementLabel, int floorNumber, BigDecimal lengthM, BigDecimal widthM, BigDecimal heightM,
        int quantity, BigDecimal amount, String baseUnit, String source, String sourceLabel,
        BigDecimal discrepancyPct, String notes, Instant createdAt) {

    public static MeasurementResponse from(BlueprintMeasurement measurement) {
        Blueprint blueprint = measurement.getBlueprint();
        return new MeasurementResponse(measurement.getId(), blueprint == null ? null : blueprint.getId(),
                blueprint == null ? null : blueprint.getOriginalFilename(), measurement.getElementType().getCode(),
                measurement.getElementType().getLabel(), measurement.getFloorNumber(), measurement.getLengthM(),
                measurement.getWidthM(), measurement.getHeightM(), measurement.getQuantity(),
                measurement.getAmount(), MeasurementRules.baseUnit(measurement.getElementType()),
                measurement.getSource().getCode(), measurement.getSource().getLabel(),
                measurement.getDiscrepancyPct(), measurement.getNotes(), measurement.getCreatedAt());
    }
}
