package com.obraia.backend.risk;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;

import com.obraia.backend.entity.risk.RiskLevel;

/**
 * Runs every {@link RiskStrategy} (Spring gives all of them, in their
 * order) and combines the results. The total is half the weighted average
 * and half the highest factor: one serious problem is enough to raise the
 * level, even when everything else is fine.
 */
@Component
public class RiskEvaluator {

    /** Name of these rules, saved with each assessment (model_version). */
    public static final String MODEL_VERSION = "rules-v1";

    private static final BigDecimal HALF = new BigDecimal("0.5");

    private final List<RiskStrategy> strategies;

    public RiskEvaluator(List<RiskStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    public RiskResult evaluate(ProjectSnapshot snapshot) {
        List<RiskFactor> factors = strategies.stream().map(strategy -> strategy.evaluate(snapshot)).toList();
        BigDecimal weights = factors.stream().map(RiskFactor::weight).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal weighted = factors.stream()
                .map(factor -> factor.score().multiply(factor.weight()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = weights.signum() == 0 ? BigDecimal.ZERO
                : weighted.divide(weights, 4, RoundingMode.HALF_UP);
        BigDecimal highest = factors.stream().map(RiskFactor::score).max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal score = RiskScores.clamp(average.multiply(HALF).add(highest.multiply(HALF)));
        int delay = factors.stream().mapToInt(RiskFactor::delayDays).max().orElse(0);
        return new RiskResult(score, RiskLevel.fromScore(score), delay, factors);
    }
}
