package com.obraia.backend.quotation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void roundsHalfUpToTwoDecimals() {
        assertEquals(new BigDecimal("10.13"), Money.round(new BigDecimal("10.125")));
        assertEquals(new BigDecimal("10.12"), Money.round(new BigDecimal("10.1249")));
    }

    @Test
    void keepsTwoDecimalsOnWholeAmounts() {
        assertEquals(new BigDecimal("5000.00"), Money.round(new BigDecimal("5000")));
    }

    @Test
    void percentOfUsesTheSameRounding() {
        assertEquals(new BigDecimal("1900.00"), Money.percentOf(new BigDecimal("10000"), new BigDecimal("19")));
        assertEquals(new BigDecimal("0.33"), Money.percentOf(new BigDecimal("6.6"), new BigDecimal("5")));
    }
}
