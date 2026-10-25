package com.obraia.backend.dto.catalog;

import java.math.BigDecimal;
import java.util.UUID;

import com.obraia.backend.entity.catalog.MaterialFormula;

/** Example: a wall (per m2) needs 12.5 units of "Bloque de arcilla No. 5", plus 5 % waste. */
public record MaterialFormulaResponse(String elementType, String elementLabel, String baseUnit,
        UUID materialId, String materialName, String materialUnit,
        BigDecimal consumptionPerUnit, BigDecimal wasteFactor) {

    public static MaterialFormulaResponse from(MaterialFormula formula) {
        return new MaterialFormulaResponse(formula.getElementType().getCode(), formula.getElementType().getLabel(),
                formula.getBaseUnit(), formula.getMaterial().getId(), formula.getMaterial().getName(),
                formula.getMaterial().getUnit(), formula.getConsumptionPerUnit(), formula.getWasteFactor());
    }
}
