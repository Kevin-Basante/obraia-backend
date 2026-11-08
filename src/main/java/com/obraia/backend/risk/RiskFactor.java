package com.obraia.backend.risk;

import java.math.BigDecimal;
import java.util.Map;

/**
 * What one strategy found.
 *
 * @param code       stable name of the strategy, such as "schedule_delay"
 * @param score      0 (no risk) to 100, with 2 decimals
 * @param weight     share of this factor in the average of the assessment
 * @param delayDays  days of delay this factor predicts; 0 when it does not predict any
 * @param detail     one sentence that explains the score
 * @param detailCode stable name of that sentence, such as "late_tasks", so the frontend can translate it
 * @param detailParams values of the sentence; empty when it has none
 */
public record RiskFactor(String code, String label, BigDecimal score, BigDecimal weight, int delayDays,
        String detail, String detailCode, Map<String, String> detailParams) {

    /** A factor without a detail code, like the ones saved before the codes existed. */
    public RiskFactor(String code, String label, BigDecimal score, BigDecimal weight, int delayDays,
            String detail) {
        this(code, label, score, weight, delayDays, detail, null, Map.of());
    }
}
