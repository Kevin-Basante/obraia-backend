package com.obraia.backend.dto.phase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Averages of progress percentages, rounded to 2 decimals. */
public final class Progress {

    private Progress() {
    }

    /** 0 when there is nothing to average. */
    public static BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }
}
