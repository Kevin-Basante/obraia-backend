package com.obraia.backend.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class RiskScoresTest {

    @Test
    void sentencesUseWholeNumbers() {
        assertEquals("38%", RiskScores.percent(new BigDecimal("37.5")));
        assertEquals("38", RiskScores.whole(new BigDecimal("37.5")));
        assertEquals("0", RiskScores.whole(BigDecimal.ZERO));
    }

    @Test
    void scoresStayBetween0And100() {
        assertEquals(RiskScores.ZERO, RiskScores.clamp(new BigDecimal("-4")));
        assertEquals(RiskScores.MAX, RiskScores.clamp(new BigDecimal("130")));
    }
}
