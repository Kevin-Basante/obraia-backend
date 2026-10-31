package com.obraia.backend.planning;

import org.springframework.stereotype.Component;

/** Calculators of a commercial unit ("Local comercial" in the catalog). */
@Component
public class CommercialUnitCalculatorFactory extends ProjectCalculatorFactory {

    @Override
    protected PhasePlanner createPhasePlanner() {
        return new CommercialUnitPhasePlanner();
    }

    @Override
    public String projectTypeName() {
        return "Local comercial";
    }
}
