package com.obraia.backend.planning;

import java.time.LocalDate;
import java.util.List;

import com.obraia.backend.entity.catalog.PhaseTemplate;
import com.obraia.backend.entity.catalog.ProjectType;
import com.obraia.backend.entity.catalog.SizeCategory;

/**
 * Factory Method pattern. Each project type has its own subclass, and
 * that subclass decides which phase planner to create. The services only
 * call {@link #planPhases}, so a new project type means a new subclass,
 * not a change in the services. The material calculator joins in block 6.
 */
public abstract class ProjectCalculatorFactory {

    /** The factory method. */
    protected abstract PhasePlanner createPhasePlanner();

    /** Name of the project type in the catalog that this factory serves. */
    public abstract String projectTypeName();

    public boolean supports(ProjectType projectType) {
        return projectTypeName().equalsIgnoreCase(projectType.getName());
    }

    public List<PlannedPhase> planPhases(List<PhaseTemplate> templates, SizeCategory size, LocalDate start) {
        return createPhasePlanner().plan(templates, size, start);
    }
}
