package com.obraia.backend.ai;

import java.math.BigDecimal;

import com.obraia.backend.entity.catalog.ElementType;

/** One element read by the AI, already in the terms of the backend. */
public record AiMeasurement(ElementType elementType, int floorNumber, BigDecimal lengthM, BigDecimal widthM,
        BigDecimal heightM, int quantity, String notes) {
}
