package com.obraia.backend.quotation;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Rounding rules of the quotation: amounts and quantities keep two decimals. */
final class Money {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private Money() {
    }

    static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** {@code pct} percent of {@code base}, rounded. */
    static BigDecimal percentOf(BigDecimal base, BigDecimal pct) {
        return round(base.multiply(pct).divide(HUNDRED));
    }
}
