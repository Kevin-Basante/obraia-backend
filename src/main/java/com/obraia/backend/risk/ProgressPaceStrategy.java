package com.obraia.backend.risk;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Progress against time: if 50% of the planned time passed and only 30%
 * of the work is done, the project goes 20 points behind. The gap, as a
 * share of the planned days, is the predicted delay.
 */
@Component
@Order(2)
public class ProgressPaceStrategy implements RiskStrategy {

    static final String CODE = "progress_pace";
    static final BigDecimal WEIGHT = new BigDecimal("0.25");
    private static final String LABEL = "Progress against time";
    private static final String DETAIL_CODE = "time_vs_progress";
    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    @Override
    public RiskFactor evaluate(ProjectSnapshot snapshot) {
        BigDecimal elapsed = snapshot.elapsedPct();
        if (elapsed == null) {
            return new RiskFactor(CODE, LABEL, RiskScores.ZERO, WEIGHT, 0, "The plan has no dates yet", "no_dates",
                    Map.of());
        }
        BigDecimal gap = elapsed.subtract(snapshot.progressPct());
        String detail = RiskScores.percent(elapsed) + " of the planned time passed with "
                + RiskScores.percent(snapshot.progressPct()) + " of the work done";
        Map<String, String> params = Map.of("elapsed", RiskScores.whole(elapsed),
                "progress", RiskScores.whole(snapshot.progressPct()));
        if (gap.signum() <= 0) {
            return new RiskFactor(CODE, LABEL, RiskScores.ZERO, WEIGHT, 0, detail, DETAIL_CODE, params);
        }
        int delayDays = gap.multiply(BigDecimal.valueOf(snapshot.plannedDays()))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).intValue();
        return new RiskFactor(CODE, LABEL, RiskScores.clamp(gap.multiply(TWO)), WEIGHT, delayDays, detail,
                DETAIL_CODE, params);
    }
}
