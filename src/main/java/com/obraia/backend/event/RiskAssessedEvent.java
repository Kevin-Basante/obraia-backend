package com.obraia.backend.event;

import java.math.BigDecimal;
import java.util.UUID;

import com.obraia.backend.entity.risk.RiskLevel;

/**
 * Published after a risk assessment is saved.
 *
 * @param previousLevel level of the assessment before this one; null for the first one
 * @param mainReason    detail of the factor with the highest score
 */
public record RiskAssessedEvent(UUID projectId, RiskLevel previousLevel, RiskLevel level, BigDecimal score,
        int predictedDelayDays, String mainReason) {
}
