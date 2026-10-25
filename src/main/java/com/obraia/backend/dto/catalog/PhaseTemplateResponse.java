package com.obraia.backend.dto.catalog;

import com.obraia.backend.entity.catalog.PhaseTemplate;

public record PhaseTemplateResponse(int order, String name, int estimatedDurationDays) {

    public static PhaseTemplateResponse from(PhaseTemplate template) {
        return new PhaseTemplateResponse(template.getSequenceOrder(), template.getName(),
                template.getEstimatedDurationDays());
    }
}
