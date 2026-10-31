package com.obraia.backend.entity.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import com.obraia.backend.exception.BadRequestException;

class RiskCodesTest {

    @ParameterizedTest
    @CsvSource({"0, LOW", "24.99, LOW", "25, MEDIUM", "49.99, MEDIUM", "50, HIGH", "74.99, HIGH", "75, CRITICAL",
            "100, CRITICAL"})
    void levelComesFromTheScore(String score, RiskLevel level) {
        assertEquals(level, RiskLevel.fromScore(new BigDecimal(score)));
    }

    @Test
    void levelsAreOrdered() {
        assertTrue(RiskLevel.CRITICAL.isAtLeast(RiskLevel.HIGH));
        assertTrue(RiskLevel.HIGH.isAtLeast(RiskLevel.HIGH));
        assertFalse(RiskLevel.MEDIUM.isAtLeast(RiskLevel.HIGH));
    }

    @Test
    void codesGoAndComeBack() {
        RiskLevelConverter levels = new RiskLevelConverter();
        RiskTriggerConverter triggers = new RiskTriggerConverter();
        for (RiskLevel level : RiskLevel.values()) {
            assertEquals(level, levels.convertToEntityAttribute(levels.convertToDatabaseColumn(level)));
        }
        for (RiskTrigger trigger : RiskTrigger.values()) {
            assertEquals(trigger, triggers.convertToEntityAttribute(triggers.convertToDatabaseColumn(trigger)));
        }
        assertEquals("progress_report", RiskTrigger.PROGRESS_REPORT.getCode());
    }

    @Test
    void unknownCodesAreRejected() {
        assertThrows(BadRequestException.class, () -> RiskLevel.fromCode("extreme"));
        assertThrows(BadRequestException.class, () -> RiskTrigger.fromCode("cron"));
    }
}
