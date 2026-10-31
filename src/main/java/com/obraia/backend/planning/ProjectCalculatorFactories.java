package com.obraia.backend.planning;

import java.util.List;

import org.springframework.stereotype.Component;

import com.obraia.backend.entity.catalog.ProjectType;
import com.obraia.backend.exception.ConflictException;

/** Finds the factory of a project type among all the ones Spring knows. */
@Component
public class ProjectCalculatorFactories {

    private final List<ProjectCalculatorFactory> factories;

    public ProjectCalculatorFactories(List<ProjectCalculatorFactory> factories) {
        this.factories = List.copyOf(factories);
    }

    public ProjectCalculatorFactory forType(ProjectType projectType) {
        return factories.stream()
                .filter(factory -> factory.supports(projectType))
                .findFirst()
                .orElseThrow(() -> new ConflictException("NO_PLANNER_FOR_TYPE",
                        "There is no planner for the project type " + projectType.getName()));
    }
}
