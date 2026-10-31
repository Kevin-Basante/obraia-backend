package com.obraia.backend.planning;

import java.util.List;

import com.obraia.backend.entity.blueprint.BlueprintMeasurement;
import com.obraia.backend.entity.catalog.MaterialFormula;
import com.obraia.backend.quotation.MaterialTakeoff;

/** Product of the factories: turns measurements into quantities of material. */
public interface MaterialCalculator {

    MaterialTakeoff calculate(List<BlueprintMeasurement> measurements, List<MaterialFormula> formulas);
}
