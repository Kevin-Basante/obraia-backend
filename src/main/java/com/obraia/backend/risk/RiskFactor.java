package com.obraia.backend.risk;

import java.math.BigDecimal;

/**
 * What one strategy found.
 *
 * @param code       stable name of the strategy, such as "schedule_delay"
 * @param score      0 (no risk) to 100, with 2 decimals
 * @param weight     share of this factor in the average of the assessment
 * @param delayDays  days of delay this factor predicts; 0 when it does not predict any
 * @param detail     one sentence that explains the score
 */
public record RiskFactor(String code, String label, BigDecimal score, BigDecimal weight, int delayDays,
        String detail) {
}
