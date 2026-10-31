package com.obraia.backend.planning;

import java.time.LocalDate;
import java.util.List;

import com.obraia.backend.entity.blueprint.BlueprintMeasurement;
import com.obraia.backend.entity.catalog.MaterialFormula;
import com.obraia.backend.entity.catalog.PhaseTemplate;
import com.obraia.backend.entity.catalog.ProjectType;
import com.obraia.backend.entity.catalog.SizeCategory;
import com.obraia.backend.quotation.MaterialTakeoff;

/**
 * Factory Method pattern. Each project type has its own subclass, and
 * that subclass decides which phase planner and which material calculator
 * to create. The services only call {@link #planPhases} and
 * {@link #calculateMaterials}, so a new project type means a new subclass,
 * not a change in the services.
 */
public abstract class ProjectCalculatorFactory {

    /** The factory method. */
    protected abstract PhasePlanner createPhasePlanner();

    /**
     * Second factory method. Both current types use the standard
     * calculator; a type with its own rules overrides it.
     */
    protected MaterialCalculator createMaterialCalculator() {
        return new StandardMaterialCalculator();
    }

    /** Name of the project type in the catalog that this factory serves. */
    public abstract String projectTypeName();

    public boolean supports(ProjectType projectType) {
        return projectTypeName().equalsIgnoreCase(projectType.getName());
    }

    public List<PlannedPhase> planPhases(List<PhaseTemplate> templates, SizeCategory size, LocalDate start) {
        return createPhasePlanner().plan(templates, size, start);
    }

    public MaterialTakeoff calculateMaterials(List<BlueprintMeasurement> measurements,
            List<MaterialFormula> formulas) {
        return createMaterialCalculator().calculate(measurements, formulas);
    }
}
