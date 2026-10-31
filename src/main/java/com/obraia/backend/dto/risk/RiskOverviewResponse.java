package com.obraia.backend.dto.risk;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.obraia.backend.entity.risk.RiskAssessment;

/**
 * The latest assessment and how the score moved.
 *
 * @param latest  null before the first assessment
 * @param history the last 20 assessments, oldest first, ready for a chart
 */
public record RiskOverviewResponse(RiskAssessmentResponse latest, List<Point> history) {

    /** One assessment in the history. */
    public record Point(Instant assessedAt, String level, BigDecimal score, int predictedDelayDays, String trigger) {

        public static Point from(RiskAssessment assessment) {
            return new Point(assessment.getAssessedAt(), assessment.getRiskLevel().getCode(),
                    assessment.getRiskScore(), assessment.getPredictedDelayDays(),
                    assessment.getTriggerSource().getCode());
        }
    }
}
