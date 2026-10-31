package com.obraia.backend.planning;

import org.springframework.stereotype.Component;

/** Calculators of a single-family house ("Vivienda unifamiliar" in the catalog). */
@Component
public class HouseCalculatorFactory extends ProjectCalculatorFactory {

    @Override
    protected PhasePlanner createPhasePlanner() {
        return new HousePhasePlanner();
    }

    @Override
    public String projectTypeName() {
        return "Vivienda unifamiliar";
    }
}
