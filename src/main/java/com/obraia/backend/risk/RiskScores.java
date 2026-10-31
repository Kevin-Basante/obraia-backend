package com.obraia.backend.risk;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Small helpers shared by the strategies. */
final class RiskScores {

    static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    static final BigDecimal MAX = new BigDecimal("100.00");

    private RiskScores() {
    }

    /** Between 0 and 100, with 2 decimals. */
    static BigDecimal clamp(BigDecimal value) {
        BigDecimal scaled = value.setScale(2, RoundingMode.HALF_UP);
        if (scaled.signum() < 0) {
            return ZERO;
        }
        return scaled.min(MAX);
    }

    /** A percentage without decimals, for the sentences: 37.5 becomes "38%". */
    static String percent(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).toPlainString() + "%";
    }
}
