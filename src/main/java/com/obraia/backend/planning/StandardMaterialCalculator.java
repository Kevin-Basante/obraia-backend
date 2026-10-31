package com.obraia.backend.planning;

import java.util.List;

import com.obraia.backend.entity.blueprint.BlueprintMeasurement;
import com.obraia.backend.entity.catalog.MaterialFormula;
import com.obraia.backend.quotation.MaterialTakeoff;

/** Measured amount of each element times the consumption of its formulas. */
public class StandardMaterialCalculator implements MaterialCalculator {

    @Override
    public MaterialTakeoff calculate(List<BlueprintMeasurement> measurements, List<MaterialFormula> formulas) {
        return MaterialTakeoff.of(measurements, formulas);
    }
}
