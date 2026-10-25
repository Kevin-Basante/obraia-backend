package com.obraia.backend.dto.catalog;

import java.util.List;
import java.util.UUID;

/**
 * Standard phases of a project type and size. The total assumes that
 * each phase starts when the previous one ends.
 */
public record PhasePlanResponse(UUID projectTypeId, String sizeCategory, int totalDurationDays,
        List<PhaseTemplateResponse> phases) {

    public static PhasePlanResponse of(UUID projectTypeId, String sizeCategory, List<PhaseTemplateResponse> phases) {
        int total = phases.stream().mapToInt(PhaseTemplateResponse::estimatedDurationDays).sum();
        return new PhasePlanResponse(projectTypeId, sizeCategory, total, phases);
    }
}
