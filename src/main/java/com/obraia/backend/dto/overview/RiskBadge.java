package com.obraia.backend.dto.overview;

import java.math.BigDecimal;
import java.time.Instant;

import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.entity.risk.RiskAssessment;

/** The latest risk assessment, without its factors. */
public record RiskBadge(StatusResponse level, BigDecimal score, int predictedDelayDays, Instant assessedAt) {

    public static RiskBadge from(RiskAssessment assessment) {
        return new RiskBadge(
                new StatusResponse(assessment.getRiskLevel().getCode(), assessment.getRiskLevel().getLabel()),
                assessment.getRiskScore(), assessment.getPredictedDelayDays(), assessment.getAssessedAt());
    }
}
