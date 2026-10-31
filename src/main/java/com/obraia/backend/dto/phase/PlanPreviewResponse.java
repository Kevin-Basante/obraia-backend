package com.obraia.backend.dto.phase;

import java.util.List;

/**
 * What "create the plan" would make for this project.
 *
 * @param projectType   name of the project type that chose the planner
 * @param workingDays   sum of the phase durations (Monday to Saturday)
 */
public record PlanPreviewResponse(String projectType, String sizeCategory, int workingDays,
        List<PlannedPhaseResponse> phases) {
}
