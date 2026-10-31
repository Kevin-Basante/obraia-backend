package com.obraia.backend.dto.risk;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.entity.risk.RiskAssessment;
import com.obraia.backend.risk.RiskFactor;

/** A saved assessment with what each strategy found. */
public record RiskAssessmentResponse(UUID id, StatusResponse level, BigDecimal score, int predictedDelayDays,
        List<RiskFactor> factors, String modelVersion, StatusResponse trigger, Instant assessedAt) {

    public static RiskAssessmentResponse from(RiskAssessment assessment, List<RiskFactor> factors) {
        return new RiskAssessmentResponse(assessment.getId(),
                new StatusResponse(assessment.getRiskLevel().getCode(), assessment.getRiskLevel().getLabel()),
                assessment.getRiskScore(), assessment.getPredictedDelayDays(), factors, assessment.getModelVersion(),
                new StatusResponse(assessment.getTriggerSource().getCode(), assessment.getTriggerSource().getLabel()),
                assessment.getAssessedAt());
    }
}
