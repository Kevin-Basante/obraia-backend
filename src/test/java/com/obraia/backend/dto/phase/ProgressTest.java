package com.obraia.backend.dto.phase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class ProgressTest {

    @Test
    void nothingToAverageIsZero() {
        assertEquals(new BigDecimal("0.00"), Progress.average(List.of()));
    }

    @Test
    void averageIsRoundedToTwoDecimals() {
        List<BigDecimal> values = List.of(new BigDecimal("40"), BigDecimal.ZERO, BigDecimal.ZERO);

        assertEquals(new BigDecimal("13.33"), Progress.average(values));
    }

    @Test
    void halfwayRoundsUp() {
        // 0.01 / 2 = 0.005
        assertEquals(new BigDecimal("0.01"), Progress.average(List.of(new BigDecimal("0.01"), BigDecimal.ZERO)));
    }
}
