package com.obraia.backend.event;

import java.math.BigDecimal;
import java.util.UUID;

import com.obraia.backend.entity.risk.RiskLevel;
import com.obraia.backend.risk.RiskFactor;

/**
 * Published after a risk assessment is saved.
 *
 * @param previousLevel level of the assessment before this one; null for the first one
 * @param mainFactor    the factor with the highest score; null when there are no factors
 */
public record RiskAssessedEvent(UUID projectId, RiskLevel previousLevel, RiskLevel level, BigDecimal score,
        int predictedDelayDays, RiskFactor mainFactor) {
}
