package com.obraia.backend.risk;

import java.math.BigDecimal;
import java.util.List;

import com.obraia.backend.entity.risk.RiskLevel;

/** The assessment of a project: total score, its level, predicted delay and what each strategy found. */
public record RiskResult(BigDecimal score, RiskLevel level, int predictedDelayDays, List<RiskFactor> factors) {
}
